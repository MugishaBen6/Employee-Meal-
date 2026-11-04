package com.emeal;

import com.emeal.dto.request.CreateEmployeeRequest;
import com.emeal.dto.request.RecordMealRequest;
import com.emeal.dto.request.UpdateMealRecordRequest;
import com.emeal.dto.response.EmployeeDTO;
import com.emeal.dto.response.MealRecordDTO;
import com.emeal.dto.response.PageResponse;
import com.emeal.dto.response.QuickMealCheckResponse;
import com.emeal.dto.response.UserDashboardStatsResponse;
import com.emeal.entity.AuditLog;
import com.emeal.entity.MealStatus;
import com.emeal.repository.AuditLogRepository;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import com.emeal.service.DashboardService;
import com.emeal.service.EmployeeService;
import com.emeal.service.MealRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end (service + H2 database) checks for the USER role rules:
 * ownership on list/get/update, no delete, server-side recordedBy,
 * personal statistics, quick-check redaction and synchronous audit history.
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class UserRoleAuthorizationTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 15);

    @Autowired private MealRecordService mealRecordService;
    @Autowired private EmployeeService employeeService;
    @Autowired private DashboardService dashboardService;
    @Autowired private MealRecordRepository mealRecordRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private EmployeeDTO worker1;
    private EmployeeDTO worker2;
    private EmployeeDTO worker3;

    @BeforeEach
    void setUp() {
        login("hr1", "HR");
        mealRecordRepository.deleteAll();
        employeeRepository.deleteAll();
        worker1 = createEmployee("UR001", "Alpha", "One");
        worker2 = createEmployee("UR002", "Beta", "Two");
        worker3 = createEmployee("UR003", "Gamma", "Three");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void user_recordsMeal_ownedByAuthenticatedPrincipal_withConfiguredPrice() {
        login("alice", "USER");

        MealRecordDTO rec = record(worker1, MealStatus.ATE, null);

        assertEquals("alice", rec.getRecordedBy());
        // V5 migration sets STANDARD_MEAL_PRICE to 600.00; a missing amount uses it.
        assertEquals(0, new BigDecimal("600.00").compareTo(rec.getAmount()));
    }

    @Test
    void didNotEat_alwaysStoresZero_andAteWithZeroUsesStandardPrice() {
        login("alice", "USER");

        MealRecordDTO dne = record(worker1, MealStatus.DID_NOT_EAT, new BigDecimal("999"));
        MealRecordDTO ateZero = record(worker2, MealStatus.ATE, BigDecimal.ZERO);

        assertEquals(0, BigDecimal.ZERO.compareTo(dne.getAmount()));
        assertEquals(0, new BigDecimal("600.00").compareTo(ateZero.getAmount()));
    }

    @Test
    void user_seesOnlyOwnRecords_inSearchHistoryAndById() {
        login("alice", "USER");
        MealRecordDTO aliceRec = record(worker1, MealStatus.ATE, null);
        login("bob", "USER");
        MealRecordDTO bobRec = record(worker2, MealStatus.ATE, null);
        record(worker1, MealStatus.ATE, null, DAY.plusDays(1));

        login("alice", "USER");
        // Even when alice asks for bob's records explicitly, the filter is forced to her.
        PageResponse<MealRecordDTO> page = mealRecordService.searchMealRecords(
                null, null, null, null, null, "bob", 0, 50);
        assertEquals(1, page.getContent().size());
        assertTrue(page.getContent().stream().allMatch(r -> "alice".equals(r.getRecordedBy())));

        List<MealRecordDTO> history = mealRecordService.getEmployeeMealHistory(worker1.getId());
        assertEquals(1, history.size());
        assertEquals(aliceRec.getId(), history.get(0).getId());

        assertEquals(aliceRec.getId(), mealRecordService.getMealRecordById(aliceRec.getId()).getId());
        assertThrows(AccessDeniedException.class, () -> mealRecordService.getMealRecordById(bobRec.getId()));
    }

    @Test
    void user_cannotEditAnotherUsersRecord_andRecordIsUnchanged() {
        login("bob", "USER");
        MealRecordDTO bobRec = record(worker2, MealStatus.ATE, null);

        login("alice", "USER");
        UpdateMealRecordRequest update = new UpdateMealRecordRequest();
        update.setMealStatus(MealStatus.DID_NOT_EAT);
        update.setAmount(BigDecimal.ZERO);
        assertThrows(AccessDeniedException.class, () -> mealRecordService.updateMealRecord(bobRec.getId(), update));

        var stored = mealRecordRepository.findById(bobRec.getId()).orElseThrow();
        assertEquals(MealStatus.ATE, stored.getMealStatus());
        assertEquals("bob", stored.getRecordedBy());
    }

    @Test
    void user_editsOwnRecord_ownershipPreserved_andAuditHasActorAndBeforeAfter() {
        login("alice", "USER");
        MealRecordDTO rec = record(worker1, MealStatus.ATE, null);

        UpdateMealRecordRequest update = new UpdateMealRecordRequest();
        update.setMealStatus(MealStatus.DID_NOT_EAT);
        update.setAmount(BigDecimal.ZERO);
        MealRecordDTO updated = mealRecordService.updateMealRecord(rec.getId(), update);

        assertEquals("alice", updated.getRecordedBy());
        assertEquals(MealStatus.DID_NOT_EAT, updated.getMealStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(updated.getAmount()));

        // Written synchronously in the same transaction, so it is visible immediately.
        AuditLog audit = auditLogRepository.findAll().stream()
                .filter(a -> "UPDATE_MEAL_RECORD".equals(a.getAction()) && rec.getId().toString().equals(a.getEntityId()))
                .findFirst().orElseThrow();
        assertEquals("alice", audit.getUsername());
        assertEquals("USER", audit.getUserRole());
        assertNotNull(audit.getTimestamp());
        assertTrue(audit.getDescription().contains("PREVIOUS: status=ATE"));
        assertTrue(audit.getDescription().contains("NEW: status=DID_NOT_EAT"));
    }

    @Test
    void user_cannotDeleteRecords() {
        login("alice", "USER");
        MealRecordDTO rec = record(worker1, MealStatus.ATE, null);

        assertThrows(AccessDeniedException.class, () -> mealRecordService.deleteMealRecord(rec.getId()));
        assertTrue(mealRecordRepository.findById(rec.getId()).isPresent());
    }

    @Test
    void quickCheck_hidesOtherAccountsRecordFromUser_butStillReportsDuplicate() {
        login("bob", "USER");
        record(worker2, MealStatus.ATE, null);

        login("alice", "USER");
        QuickMealCheckResponse forAlice = mealRecordService.quickCheck(worker2.getId().toString(), DAY);
        assertTrue(forAlice.isAlreadyRecordedToday());
        assertNull(forAlice.getTodayRecord());
        assertEquals(0, new BigDecimal("600.00").compareTo(forAlice.getDefaultMealPrice()));

        login("admin1", "ADMIN");
        QuickMealCheckResponse forAdmin = mealRecordService.quickCheck(worker2.getId().toString(), DAY);
        assertNotNull(forAdmin.getTodayRecord());
        assertEquals("bob", forAdmin.getTodayRecord().getRecordedBy());
    }

    @Test
    void personalStatistics_countOnlyCallersRecords_andExposeStandardPrice() {
        login("alice", "USER");
        record(worker1, MealStatus.ATE, null);
        record(worker2, MealStatus.DID_NOT_EAT, null);
        login("bob", "USER");
        record(worker3, MealStatus.ATE, null);

        login("alice", "USER");
        UserDashboardStatsResponse stats = dashboardService.getCurrentUserStatistics();
        assertEquals("alice", stats.getUsername());
        assertEquals(2, stats.getTotalRecords());
        assertEquals(1, stats.getAteCount());
        assertEquals(1, stats.getDidNotEatCount());
        assertEquals(0, new BigDecimal("600.00").compareTo(stats.getTotalAmount()));
        assertEquals(0, new BigDecimal("600.00").compareTo(stats.getStandardMealPrice()));
    }

    @Test
    void nonUserRoles_keepFullVisibility_andPersonalStatsAreUserOnly() {
        login("alice", "USER");
        record(worker1, MealStatus.ATE, null);
        login("bob", "USER");
        record(worker2, MealStatus.ATE, null);

        for (String role : List.of("ADMIN", "MANAGING_DIRECTOR", "ACCOUNTANT", "HR")) {
            login("staff_" + role, role);
            PageResponse<MealRecordDTO> all = mealRecordService.searchMealRecords(
                    null, null, null, null, null, null, 0, 50);
            assertEquals(2, all.getContent().size(), role + " should see all records");
            assertThrows(AccessDeniedException.class, () -> dashboardService.getCurrentUserStatistics(),
                    role + " must not use the USER-only personal statistics");
        }
    }

    @Test
    void admin_canEditAnyRecord_withoutTakingOwnership() {
        login("alice", "USER");
        MealRecordDTO rec = record(worker1, MealStatus.ATE, null);

        login("admin1", "ADMIN");
        UpdateMealRecordRequest update = new UpdateMealRecordRequest();
        update.setMealStatus(MealStatus.ATE);
        update.setAmount(new BigDecimal("700.00"));
        MealRecordDTO updated = mealRecordService.updateMealRecord(rec.getId(), update);

        assertEquals("alice", updated.getRecordedBy());
        assertEquals(0, new BigDecimal("700.00").compareTo(updated.getAmount()));
    }

    @Test
    void recordingWithoutAuthentication_failsAndPersistsNothing() {
        SecurityContextHolder.clearContext();
        RecordMealRequest req = new RecordMealRequest();
        req.setEmployeeId(worker1.getId());
        req.setMealDate(DAY);
        req.setMealStatus(MealStatus.ATE);

        assertThrows(IllegalStateException.class, () -> mealRecordService.recordMeal(req));
        assertEquals(0, mealRecordRepository.count());
    }

    // --- helpers ---

    private static void login(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private EmployeeDTO createEmployee(String code, String first, String last) {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setEmployeeCode(code);
        req.setFirstName(first);
        req.setLastName(last);
        req.setDepartment("Production");
        req.setPosition("Worker");
        req.setPhone("+2507880" + code.substring(2) + "00");
        return employeeService.createEmployee(req);
    }

    private MealRecordDTO record(EmployeeDTO emp, MealStatus status, BigDecimal amount) {
        return record(emp, status, amount, DAY);
    }

    private MealRecordDTO record(EmployeeDTO emp, MealStatus status, BigDecimal amount, LocalDate date) {
        RecordMealRequest req = new RecordMealRequest();
        req.setEmployeeId(emp.getId());
        req.setMealDate(date);
        req.setMealStatus(status);
        req.setAmount(amount);
        return mealRecordService.recordMeal(req);
    }
}
