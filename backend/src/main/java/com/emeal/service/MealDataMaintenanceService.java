package com.emeal.service;

import com.emeal.config.BusinessTime;
import com.emeal.exception.BadRequestException;
import com.emeal.exception.DuplicateResourceException;
import com.emeal.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ADMIN data maintenance for meal records: an exact summary, a full CSV backup, and a guarded
 * bulk delete. A delete only runs when the caller confirms it and the number of records still
 * matches the summary the caller saw, so a stale screen can never delete more than intended.
 */
@Service
public class MealDataMaintenanceService {

    public static final String CONFIRM_WORD = "DELETE";

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;
    private final TrashService trashService;

    public MealDataMaintenanceService(JdbcTemplate jdbcTemplate, AuditLogService auditLogService, TrashService trashService) {
        this.trashService = trashService;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    /** Inclusive meal-date range; both null means every meal record. */
    public record Scope(LocalDate start, LocalDate end) {
        boolean all() {
            return start == null;
        }

        String where() {
            return all() ? "" : " WHERE m.meal_date BETWEEN ? AND ?";
        }

        Object[] args() {
            return all() ? new Object[0] : new Object[]{Date.valueOf(start), Date.valueOf(end)};
        }

        String label() {
            return all() ? "all meal dates" : "meal dates " + start + " to " + end;
        }
    }

    public Scope scope(LocalDate start, LocalDate end) {
        if ((start == null) != (end == null)) {
            throw new BadRequestException("Provide both From Date and To Date, or neither to cover all records.");
        }
        if (start != null && start.isAfter(end)) {
            throw new BadRequestException("From Date must be on or before To Date.");
        }
        return new Scope(start, end);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(Scope scope) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS total, " +
                "COALESCE(SUM(CASE WHEN m.meal_status = 'ATE' THEN 1 ELSE 0 END), 0) AS ate, " +
                "COALESCE(SUM(CASE WHEN m.meal_status = 'DID_NOT_EAT' THEN 1 ELSE 0 END), 0) AS dne, " +
                "COALESCE(SUM(CASE WHEN m.meal_status = 'ATE' THEN m.amount ELSE 0 END), 0) AS amount, " +
                "MIN(m.meal_date) AS first_date, MAX(m.meal_date) AS last_date " +
                "FROM meal_records m" + scope.where(), scope.args());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalRecords", ((Number) row.get("total")).longValue());
        out.put("ateCount", ((Number) row.get("ate")).longValue());
        out.put("didNotEatCount", ((Number) row.get("dne")).longValue());
        out.put("totalAmount", new BigDecimal(row.get("amount").toString()));
        out.put("firstMealDate", row.get("first_date") == null ? null : row.get("first_date").toString());
        out.put("lastMealDate", row.get("last_date") == null ? null : row.get("last_date").toString());
        out.put("startDate", scope.start());
        out.put("endDate", scope.end());
        return out;
    }

    /** Every column of every record in scope, as UTF-8 CSV (with BOM so Excel shows names correctly). */
    @Transactional // not read-only: the download itself is written to the audit log
    public byte[] backupCsv(Scope scope) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("id,employee_id,employee_code,employee_name,department,meal_date,meal_status,amount,recorded_by,source,created_at,updated_at,imported_at\n");
        jdbcTemplate.query(
                "SELECT m.id, m.employee_id, e.employee_code, TRIM(e.first_name || ' ' || e.last_name) AS name, e.department, " +
                "m.meal_date, m.meal_status, m.amount, m.recorded_by, m.source, m.created_at, m.updated_at, m.imported_at " +
                "FROM meal_records m JOIN employees e ON e.id = m.employee_id" + scope.where() +
                " ORDER BY m.meal_date, e.employee_code, m.id",
                rs -> {
                    for (int i = 1; i <= 13; i++) {
                        if (i > 1) sb.append(',');
                        Object v = rs.getObject(i);
                        sb.append(csv(v == null ? "" : (v instanceof BigDecimal b ? b.toPlainString() : v.toString())));
                    }
                    sb.append('\n');
                },
                scope.args());
        auditLogService.logActionSync("BACKUP_MEAL_RECORDS", "MEAL_RECORD", "BULK",
                "Downloaded a CSV backup of meal records (" + scope.label() + ").");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public Map<String, Object> delete(Scope scope, long expectedCount, String confirm) {
        if (!CONFIRM_WORD.equals(confirm)) {
            throw new BadRequestException("Type " + CONFIRM_WORD + " to confirm deleting meal records.");
        }
        Map<String, Object> before = summary(scope);
        long count = (Long) before.get("totalRecords");
        if (count != expectedCount) {
            throw new DuplicateResourceException("The number of meal records changed (now " + count + ", you confirmed "
                    + expectedCount + "). Refresh the summary and download a new backup before deleting.");
        }
        // Moved to the ADMIN Trash (recoverable), not destroyed.
        List<Long> ids = jdbcTemplate.queryForList("SELECT m.id FROM meal_records m" + scope.where(), Long.class, scope.args());
        int deleted = trashService.trashMealRecords(ids, "Meal data maintenance, " + scope.label());
        if (deleted != count) {
            throw new IllegalStateException("Delete affected an unexpected number of rows; nothing was deleted.");
        }
        auditLogService.logActionSync("BULK_DELETE_MEAL_RECORDS", "MEAL_RECORD", "BULK",
                "Moved to Trash " + deleted + " meal records (" + scope.label() + "): " + before.get("ateCount") + " ATE, "
                        + before.get("didNotEatCount") + " DID_NOT_EAT, total amount " + ((BigDecimal) before.get("totalAmount")).toPlainString()
                        + " RWF, meal dates " + before.get("firstMealDate") + " to " + before.get("lastMealDate")
                        + ". Requested by " + SecurityUtils.getCurrentUsername() + " at " + BusinessTime.nowForDisplay().withNano(0) + ".");
        Map<String, Object> out = new LinkedHashMap<>(before);
        out.put("deletedRecords", deleted);
        return out;
    }

    private static String csv(String v) {
        String safe = v;
        // Neutralise spreadsheet formulas in text fields (CSV injection).
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0 && !safe.matches("-?\\d+(\\.\\d+)?")) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
