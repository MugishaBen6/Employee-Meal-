package com.emeal.service;

import com.emeal.config.BusinessTime;
import com.emeal.dto.response.PageResponse;
import com.emeal.exception.BadRequestException;
import com.emeal.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Recycle Bin for employees and meal records.
 *
 * <p>Deleting moves a complete snapshot of the row(s) into {@code trash_items} and removes them from
 * the active tables in the same transaction. Every list, dashboard, report and export therefore
 * ignores deleted data without needing a filter. An employee is trashed together with all of its
 * meal records and restored together with them. Restoring re-inserts the rows with their original
 * ids, dates, amounts, statuses, owners and timestamps; anything that would create a duplicate
 * (an employee code or an employee/day meal slot taken in the meantime) is refused for that item.
 * Only ADMIN can view, restore or permanently delete Trash items (enforced in TrashController).
 */
@Service
public class TrashService {

    private static final Logger logger = LoggerFactory.getLogger(TrashService.class);

    public static final String EMPLOYEE = "EMPLOYEE";
    public static final String MEAL_RECORD = "MEAL_RECORD";
    private static final int CHUNK = 500;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final TransactionTemplate itemTx;

    public TrashService(JdbcTemplate jdbc, ObjectMapper objectMapper, AuditLogService auditLogService,
                        PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.itemTx = new TransactionTemplate(transactionManager);
        this.itemTx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    /* ======================================================================
       Snapshots (exact copies of the database rows)
       ====================================================================== */

    public record EmployeeSnap(Long id, String employeeCode, String firstName, String lastName, String department,
                               String position, String phone, String email, String status, String createdBy,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        String fullName() {
            return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        }
    }

    public record MealSnap(Long id, Long employeeId, LocalDate mealDate, String mealStatus, BigDecimal amount,
                           String recordedBy, String source, LocalDateTime importedAt,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record EmployeePayload(EmployeeSnap employee, List<MealSnap> meals) {
    }

    public record MealPayload(MealSnap meal) {
    }

    private static final String EMP_COLS = "id, employee_code, first_name, last_name, department, position, phone, email, status, created_by, created_at, updated_at";
    private static final String MEAL_COLS = "id, employee_id, meal_date, meal_status, amount, recorded_by, source, imported_at, created_at, updated_at";

    private static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    private static EmployeeSnap empRow(ResultSet rs, int i) throws SQLException {
        return new EmployeeSnap(rs.getLong("id"), rs.getString("employee_code"), rs.getString("first_name"),
                rs.getString("last_name"), rs.getString("department"), rs.getString("position"), rs.getString("phone"),
                rs.getString("email"), rs.getString("status"), rs.getString("created_by"),
                ts(rs, "created_at"), ts(rs, "updated_at"));
    }

    private static MealSnap mealRow(ResultSet rs, int i) throws SQLException {
        return new MealSnap(rs.getLong("id"), rs.getLong("employee_id"), rs.getDate("meal_date").toLocalDate(),
                rs.getString("meal_status"), rs.getBigDecimal("amount"), rs.getString("recorded_by"),
                rs.getString("source"), ts(rs, "imported_at"), ts(rs, "created_at"), ts(rs, "updated_at"));
    }

    private static String placeholders(int n) {
        return String.join(",", Collections.nCopies(n, "?"));
    }

    private static List<List<Long>> chunks(Collection<Long> ids) {
        List<Long> list = ids.stream().filter(Objects::nonNull).distinct().toList();
        List<List<Long>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i += CHUNK) out.add(list.subList(i, Math.min(i + CHUNK, list.size())));
        return out;
    }

    private static BigDecimal ateSum(List<MealSnap> meals) {
        return meals.stream().filter(m -> "ATE".equals(m.mealStatus()))
                .map(MealSnap::amount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String json(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("Could not snapshot record for Trash", e);
        }
    }

    /* ======================================================================
       Move to Trash
       ====================================================================== */

    /** Moves employees (with all their meal records) to Trash. Returns how many were moved. */
    @Transactional(propagation = Propagation.REQUIRED)
    public int trashEmployees(Collection<Long> employeeIds, String reason) {
        String actor = SecurityUtils.getCurrentUsername();
        Timestamp now = Timestamp.valueOf(BusinessTime.nowForDisplay().withNano(0));
        String batch = UUID.randomUUID().toString();
        int moved = 0;
        for (List<Long> chunk : chunks(employeeIds)) {
            Object[] args = chunk.toArray();
            List<EmployeeSnap> emps = jdbc.query("SELECT " + EMP_COLS + " FROM employees WHERE id IN (" + placeholders(chunk.size()) + ")",
                    TrashService::empRow, args);
            if (emps.isEmpty()) continue;
            Map<Long, List<MealSnap>> mealsByEmp = new HashMap<>();
            jdbc.query("SELECT " + MEAL_COLS + " FROM meal_records WHERE employee_id IN (" + placeholders(chunk.size()) + ") ORDER BY meal_date, id",
                    (ResultSet rs) -> {
                        MealSnap m = mealRow(rs, 0);
                        mealsByEmp.computeIfAbsent(m.employeeId(), k -> new ArrayList<>()).add(m);
                    }, args);

            List<Object[]> rows = new ArrayList<>();
            for (EmployeeSnap e : emps) {
                List<MealSnap> meals = mealsByEmp.getOrDefault(e.id(), List.of());
                rows.add(new Object[]{EMPLOYEE, e.id(), e.id(), e.employeeCode(), e.fullName(), null, null,
                        meals.size(), ateSum(meals), json(new EmployeePayload(e, meals)), actor, now, batch});
            }
            insertTrashRows(rows);

            Object[] empIds = emps.stream().map(EmployeeSnap::id).toArray();
            jdbc.update("DELETE FROM meal_records WHERE employee_id IN (" + placeholders(empIds.length) + ")", empIds);
            int deleted = jdbc.update("DELETE FROM employees WHERE id IN (" + placeholders(empIds.length) + ")", empIds);
            if (deleted != emps.size()) {
                throw new IllegalStateException("Employees changed while moving them to Trash; nothing was deleted.");
            }
            for (EmployeeSnap e : emps) {
                List<MealSnap> meals = mealsByEmp.getOrDefault(e.id(), List.of());
                auditLogService.logActionSync("TRASH_EMPLOYEE", "EMPLOYEE", e.id().toString(),
                        "Moved employee " + e.employeeCode() + " (" + e.fullName() + ", " + e.position() + ", " + e.status()
                                + ") to Trash with " + meals.size() + " meal record(s), ATE total " + ateSum(meals).toPlainString()
                                + " RWF" + (reason == null ? "" : " [" + reason + "]"));
            }
            moved += emps.size();
        }
        return moved;
    }

    /** Moves meal records to Trash. Returns how many were moved. */
    @Transactional(propagation = Propagation.REQUIRED)
    public int trashMealRecords(Collection<Long> mealIds, String reason) {
        String actor = SecurityUtils.getCurrentUsername();
        Timestamp now = Timestamp.valueOf(BusinessTime.nowForDisplay().withNano(0));
        String batch = UUID.randomUUID().toString();
        int moved = 0;
        for (List<Long> chunk : chunks(mealIds)) {
            Object[] args = chunk.toArray();
            List<MealSnap> meals = jdbc.query("SELECT " + MEAL_COLS + " FROM meal_records WHERE id IN (" + placeholders(chunk.size()) + ")",
                    TrashService::mealRow, args);
            if (meals.isEmpty()) continue;
            Set<Long> empIds = new HashSet<>();
            meals.forEach(m -> empIds.add(m.employeeId()));
            Map<Long, String[]> empInfo = new HashMap<>();
            jdbc.query("SELECT id, employee_code, TRIM(first_name || ' ' || last_name) AS name FROM employees WHERE id IN ("
                            + placeholders(empIds.size()) + ")",
                    (ResultSet rs) -> { empInfo.put(rs.getLong(1), new String[]{rs.getString(2), rs.getString(3)}); },
                    empIds.toArray());

            List<Object[]> rows = new ArrayList<>();
            for (MealSnap m : meals) {
                String[] info = empInfo.getOrDefault(m.employeeId(), new String[]{null, null});
                BigDecimal amount = "ATE".equals(m.mealStatus()) && m.amount() != null ? m.amount() : BigDecimal.ZERO;
                rows.add(new Object[]{MEAL_RECORD, m.id(), m.employeeId(), info[0], info[1], Date.valueOf(m.mealDate()),
                        m.mealStatus(), 1, amount, json(new MealPayload(m)), actor, now, batch});
            }
            insertTrashRows(rows);
            Object[] ids = meals.stream().map(MealSnap::id).toArray();
            int deleted = jdbc.update("DELETE FROM meal_records WHERE id IN (" + placeholders(ids.length) + ")", ids);
            if (deleted != meals.size()) {
                throw new IllegalStateException("Meal records changed while moving them to Trash; nothing was deleted.");
            }
            if (meals.size() <= 50) {
                for (MealSnap m : meals) {
                    String[] info = empInfo.getOrDefault(m.employeeId(), new String[]{"?", "?"});
                    auditLogService.logActionSync("TRASH_MEAL_RECORD", "MEAL_RECORD", m.id().toString(),
                            "Moved meal record ID " + m.id() + " to Trash: " + info[0] + " (" + info[1] + ") on " + m.mealDate()
                                    + ", status " + m.mealStatus() + ", amount " + m.amount().toPlainString() + " RWF, recorded by "
                                    + m.recordedBy() + (reason == null ? "" : " [" + reason + "]"));
                }
            } else {
                LocalDate first = meals.stream().map(MealSnap::mealDate).min(Comparator.naturalOrder()).orElseThrow();
                LocalDate last = meals.stream().map(MealSnap::mealDate).max(Comparator.naturalOrder()).orElseThrow();
                long ate = meals.stream().filter(m -> "ATE".equals(m.mealStatus())).count();
                auditLogService.logActionSync("TRASH_MEAL_RECORDS", "MEAL_RECORD", "BULK",
                        "Moved " + meals.size() + " meal records to Trash (" + ate + " ATE, " + (meals.size() - ate)
                                + " DID_NOT_EAT, ATE total " + ateSum(meals).toPlainString() + " RWF, meal dates " + first + " to " + last
                                + ", batch " + batch + ")" + (reason == null ? "" : " [" + reason + "]"));
            }
            moved += meals.size();
        }
        return moved;
    }

    private void insertTrashRows(List<Object[]> rows) {
        jdbc.batchUpdate("INSERT INTO trash_items (item_type, original_id, employee_id, employee_code, employee_name, meal_date, "
                + "meal_status, meal_count, meal_amount, payload, deleted_by, deleted_at, batch_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", rows);
    }

    /* ======================================================================
       Listing
       ====================================================================== */

    public record TrashItemDTO(Long id, String itemType, Long originalId, Long employeeId, String employeeCode,
                               String employeeName, LocalDate mealDate, String mealStatus, int mealCount,
                               BigDecimal mealAmount, String position, String department, String recordedBy,
                               String deletedBy, LocalDateTime deletedAt, boolean employeeMissing) {
    }

    private static String where(String type, String query, List<Object> args) {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
            w.append(" AND item_type = ?");
            args.add(type.trim().toUpperCase(Locale.ROOT));
        }
        if (query != null && !query.isBlank()) {
            w.append(" AND (LOWER(COALESCE(employee_name, '')) LIKE ? OR LOWER(COALESCE(employee_code, '')) LIKE ? OR LOWER(deleted_by) LIKE ?)");
            String q = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(q);
            args.add(q);
            args.add(q);
        }
        return w.toString();
    }

    @Transactional(readOnly = true)
    public PageResponse<TrashItemDTO> list(String type, String query, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 200));
        int safePage = Math.max(0, page);
        List<Object> args = new ArrayList<>();
        String where = where(type, query, args);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM trash_items" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add((long) safePage * safeSize);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM trash_items" + where + " ORDER BY deleted_at DESC, id DESC LIMIT ? OFFSET ?", pageArgs.toArray());

        Set<Long> empIds = new HashSet<>();
        rows.forEach(r -> { if (r.get("employee_id") != null) empIds.add(((Number) r.get("employee_id")).longValue()); });
        Set<Long> existing = new HashSet<>();
        if (!empIds.isEmpty()) {
            existing.addAll(jdbc.queryForList("SELECT id FROM employees WHERE id IN (" + placeholders(empIds.size()) + ")",
                    Long.class, empIds.toArray()));
        }

        List<TrashItemDTO> items = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String itemType = (String) r.get("item_type");
            String position = null, department = null, recordedBy = null;
            try {
                if (EMPLOYEE.equals(itemType)) {
                    EmployeePayload p = objectMapper.readValue((String) r.get("payload"), EmployeePayload.class);
                    position = p.employee().position();
                    department = p.employee().department();
                } else {
                    recordedBy = objectMapper.readValue((String) r.get("payload"), MealPayload.class).meal().recordedBy();
                }
            } catch (Exception e) {
                logger.warn("Unreadable Trash payload for item {}", r.get("id"));
            }
            Long employeeId = r.get("employee_id") == null ? null : ((Number) r.get("employee_id")).longValue();
            Object md = r.get("meal_date");
            Object da = r.get("deleted_at");
            items.add(new TrashItemDTO(((Number) r.get("id")).longValue(), itemType, ((Number) r.get("original_id")).longValue(),
                    employeeId, (String) r.get("employee_code"), (String) r.get("employee_name"),
                    md == null ? null : ((Date) md).toLocalDate(), (String) r.get("meal_status"),
                    ((Number) r.get("meal_count")).intValue(), new BigDecimal(r.get("meal_amount").toString()),
                    position, department, recordedBy, (String) r.get("deleted_by"),
                    da == null ? null : ((Timestamp) da).toLocalDateTime(),
                    MEAL_RECORD.equals(itemType) && employeeId != null && !existing.contains(employeeId)));
        }
        int totalPages = (int) Math.max(1, Math.ceil(total / (double) safeSize));
        return PageResponse.<TrashItemDTO>builder()
                .content(items)
                .pageNo(safePage)
                .pageSize(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(safePage >= totalPages - 1)
                .build();
    }

    /** Every Trash item id matching the filters (for "select all" across pages). */
    @Transactional(readOnly = true)
    public List<Long> ids(String type, String query) {
        List<Object> args = new ArrayList<>();
        return jdbc.queryForList("SELECT id FROM trash_items" + where(type, query, args) + " ORDER BY deleted_at DESC, id DESC",
                Long.class, args.toArray());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("employees", jdbc.queryForObject("SELECT COUNT(*) FROM trash_items WHERE item_type = 'EMPLOYEE'", Long.class));
        out.put("mealRecords", jdbc.queryForObject("SELECT COUNT(*) FROM trash_items WHERE item_type = 'MEAL_RECORD'", Long.class));
        out.put("mealsInsideEmployees", jdbc.queryForObject("SELECT COALESCE(SUM(meal_count), 0) FROM trash_items WHERE item_type = 'EMPLOYEE'", Long.class));
        out.put("ateAmount", jdbc.queryForObject("SELECT COALESCE(SUM(meal_amount), 0) FROM trash_items", BigDecimal.class));
        return out;
    }

    /* ======================================================================
       Restore
       ====================================================================== */

    public record ItemResult(Long id, boolean ok, String message) {
    }

    /**
     * Restores items one by one (each in its own transaction), employees first so that meal records
     * deleted separately can follow their employee in the same request. A refused item stays in Trash.
     */
    public Map<String, Object> restore(List<Long> trashIds) {
        if (trashIds == null || trashIds.isEmpty()) {
            throw new BadRequestException("Select at least one item to restore.");
        }
        List<Long> ids = trashIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, String> types = new HashMap<>();
        for (List<Long> chunk : chunks(ids)) {
            jdbc.query("SELECT id, item_type FROM trash_items WHERE id IN (" + placeholders(chunk.size()) + ")",
                    (ResultSet rs) -> { types.put(rs.getLong(1), rs.getString(2)); }, chunk.toArray());
        }
        List<Long> ordered = new ArrayList<>(ids);
        ordered.sort(Comparator.comparing((Long id) -> EMPLOYEE.equals(types.get(id)) ? 0 : 1));

        String actor = SecurityUtils.getCurrentUsername();
        List<ItemResult> results = new ArrayList<>();
        int restored = 0;
        for (Long id : ordered) {
            if (!types.containsKey(id)) {
                results.add(new ItemResult(id, false, "Not in Trash (already restored or permanently deleted)."));
                continue;
            }
            try {
                String msg = itemTx.execute(st -> restoreOne(id, actor));
                results.add(new ItemResult(id, true, msg));
                restored++;
            } catch (BadRequestException e) {
                results.add(new ItemResult(id, false, e.getMessage()));
            } catch (Exception e) {
                logger.error("Restore of Trash item {} failed ({})", id, e.getClass().getSimpleName());
                results.add(new ItemResult(id, false, "Could not be restored because it conflicts with current data."));
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("restored", restored);
        out.put("failed", results.size() - restored);
        out.put("results", results);
        return out;
    }

    private String restoreOne(Long trashId, String actor) {
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM trash_items WHERE id = ? FOR UPDATE", trashId);
        String type = (String) row.get("item_type");
        String payload = (String) row.get("payload");
        String deletedBy = (String) row.get("deleted_by");
        Object deletedAt = row.get("deleted_at");
        try {
            if (EMPLOYEE.equals(type)) {
                EmployeePayload p = objectMapper.readValue(payload, EmployeePayload.class);
                EmployeeSnap e = p.employee();
                Integer sameId = jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE id = ?", Integer.class, e.id());
                if (sameId != null && sameId > 0) {
                    throw new BadRequestException(e.employeeCode() + " (" + e.fullName() + ") is already in the employee list.");
                }
                List<String> codeOwner = jdbc.queryForList(
                        "SELECT TRIM(first_name || ' ' || last_name) FROM employees WHERE employee_code = ?", String.class, e.employeeCode());
                if (!codeOwner.isEmpty()) {
                    throw new BadRequestException("Employee ID " + e.employeeCode() + " is now used by " + codeOwner.get(0)
                            + ". " + e.fullName() + " cannot be restored with the same ID.");
                }
                jdbc.update("INSERT INTO employees (" + EMP_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        e.id(), e.employeeCode(), e.firstName(), e.lastName(), e.department(), e.position(), e.phone(),
                        e.email(), e.status(), e.createdBy(), tsOrNow(e.createdAt()), tsOrNow(e.updatedAt()));
                insertMeals(p.meals());
                jdbc.update("DELETE FROM trash_items WHERE id = ?", trashId);
                auditLogService.logActionSync("RESTORE_EMPLOYEE", "EMPLOYEE", e.id().toString(),
                        "Restored employee " + e.employeeCode() + " (" + e.fullName() + ") with " + p.meals().size()
                                + " meal record(s), ATE total " + ateSum(p.meals()).toPlainString() + " RWF. Deleted by "
                                + deletedBy + " at " + deletedAt + "; restored by " + actor + ".");
                return "Restored " + e.employeeCode() + " " + e.fullName() + " with " + p.meals().size() + " meal record(s).";
            }

            MealSnap m = objectMapper.readValue(payload, MealPayload.class).meal();
            String code = (String) row.get("employee_code");
            String name = (String) row.get("employee_name");
            Integer empExists = jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE id = ?", Integer.class, m.employeeId());
            if (empExists == null || empExists == 0) {
                Integer empInTrash = jdbc.queryForObject("SELECT COUNT(*) FROM trash_items WHERE item_type = 'EMPLOYEE' AND original_id = ?",
                        Integer.class, m.employeeId());
                throw new BadRequestException(empInTrash != null && empInTrash > 0
                        ? "Restore employee " + code + " (" + name + ") first; it is also in Trash."
                        : "Employee " + code + " (" + name + ") no longer exists.");
            }
            List<Map<String, Object>> slot = jdbc.queryForList(
                    "SELECT meal_status, amount FROM meal_records WHERE employee_id = ? AND meal_date = ?", m.employeeId(), Date.valueOf(m.mealDate()));
            if (!slot.isEmpty()) {
                throw new BadRequestException("A meal record already exists for " + name + " on " + m.mealDate() + " ("
                        + slot.get(0).get("meal_status") + ", " + new BigDecimal(slot.get(0).get("amount").toString()).toPlainString()
                        + " RWF). Delete that one first if this deleted record is the correct one.");
            }
            insertMeals(List.of(m));
            jdbc.update("DELETE FROM trash_items WHERE id = ?", trashId);
            auditLogService.logActionSync("RESTORE_MEAL_RECORD", "MEAL_RECORD", m.id().toString(),
                    "Restored meal record ID " + m.id() + ": " + code + " (" + name + ") on " + m.mealDate() + ", status "
                            + m.mealStatus() + ", amount " + m.amount().toPlainString() + " RWF, recorded by " + m.recordedBy()
                            + ". Deleted by " + deletedBy + " at " + deletedAt + "; restored by " + actor + ".");
            return "Restored meal record for " + name + " on " + m.mealDate() + ".";
        } catch (BadRequestException e) {
            throw e;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BadRequestException("This Trash item is unreadable and cannot be restored.");
        }
    }

    private void insertMeals(List<MealSnap> meals) {
        if (meals.isEmpty()) return;
        List<Object[]> rows = new ArrayList<>();
        for (MealSnap m : meals) {
            rows.add(new Object[]{m.id(), m.employeeId(), Date.valueOf(m.mealDate()), m.mealStatus(), m.amount(), m.recordedBy(),
                    m.source(), m.importedAt() == null ? null : Timestamp.valueOf(m.importedAt()), tsOrNow(m.createdAt()), tsOrNow(m.updatedAt())});
        }
        jdbc.batchUpdate("INSERT INTO meal_records (" + MEAL_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", rows);
    }

    private static Timestamp tsOrNow(LocalDateTime t) {
        return Timestamp.valueOf(t != null ? t : BusinessTime.nowForDisplay());
    }

    /* ======================================================================
       Permanent delete (ADMIN, irreversible)
       ====================================================================== */

    @Transactional
    public int purge(List<Long> trashIds) {
        if (trashIds == null || trashIds.isEmpty()) {
            throw new BadRequestException("Select at least one item to delete permanently.");
        }
        int purged = 0;
        for (List<Long> chunk : chunks(trashIds)) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT id, item_type, original_id, employee_code, employee_name, meal_date, meal_status, meal_count, meal_amount, deleted_by, deleted_at "
                            + "FROM trash_items WHERE id IN (" + placeholders(chunk.size()) + ")", chunk.toArray());
            if (rows.isEmpty()) continue;
            purged += jdbc.update("DELETE FROM trash_items WHERE id IN (" + placeholders(chunk.size()) + ")", chunk.toArray());
            if (rows.size() <= 50) {
                for (Map<String, Object> r : rows) {
                    boolean emp = EMPLOYEE.equals(r.get("item_type"));
                    auditLogService.logActionSync(emp ? "PERMANENT_DELETE_EMPLOYEE" : "PERMANENT_DELETE_MEAL_RECORD",
                            emp ? "EMPLOYEE" : "MEAL_RECORD", String.valueOf(r.get("original_id")),
                            "Permanently deleted from Trash: " + (emp
                                    ? "employee " + r.get("employee_code") + " (" + r.get("employee_name") + ") with " + r.get("meal_count") + " meal record(s)"
                                    : "meal record of " + r.get("employee_code") + " (" + r.get("employee_name") + ") on " + r.get("meal_date")
                                    + ", " + r.get("meal_status")) + ", ATE amount " + r.get("meal_amount") + " RWF. Originally deleted by "
                                    + r.get("deleted_by") + " at " + r.get("deleted_at") + ".");
                }
            } else {
                long emps = rows.stream().filter(r -> EMPLOYEE.equals(r.get("item_type"))).count();
                auditLogService.logActionSync("PERMANENT_DELETE_TRASH", "TRASH", "BULK",
                        "Permanently deleted " + rows.size() + " Trash item(s): " + emps + " employee(s), " + (rows.size() - emps) + " meal record(s).");
            }
        }
        return purged;
    }
}
