package com.emeal;

import com.emeal.dto.response.DashboardStatsResponse;
import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import com.emeal.service.DashboardService;
import com.emeal.service.SettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dashboard all-time totals must be the sum of stored amounts over every ATE record,
 * including employees deactivated later, and must not depend on the current price.
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class DashboardTotalsTest {

    @Autowired private DashboardService dashboardService;
    @Autowired private SettingsService settingsService;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private MealRecordRepository mealRecordRepository;

    private Employee active;
    private Employee deactivated;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        mealRecordRepository.deleteAll();
        employeeRepository.deleteAll();
        active = employee("DT001", EmployeeStatus.ACTIVE);
        deactivated = employee("DT002", EmployeeStatus.ACTIVE);

        record(active, LocalDate.of(2026, 8, 10), MealStatus.ATE, "1500.00");      // historical price
        record(active, LocalDate.of(2026, 10, 1), MealStatus.ATE, "600.00");
        record(active, LocalDate.of(2026, 10, 2), MealStatus.DID_NOT_EAT, "0.00");
        record(deactivated, LocalDate.of(2026, 8, 11), MealStatus.ATE, "1500.00"); // historical price
        record(deactivated, LocalDate.of(2026, 9, 1), MealStatus.ATE, "600.00");

        // The employee leaves after eating; their history must still count.
        deactivated.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(deactivated);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void totalMealCost_isSumOfStoredAmounts_includingDeactivatedEmployees() {
        DashboardStatsResponse stats = dashboardService.getDashboardStatistics();

        // 1500 + 600 + 1500 + 600. The old formula (active-employee ATE count x current
        // price) would have reported 2 x 600 = 1200.
        assertEquals(0, new BigDecimal("4200.00").compareTo(stats.getTotalExpenseCost()));
        assertEquals(4, stats.getTotalMealsCount(), "ATE meals of the deactivated employee are included");
        assertEquals(1, stats.getTotalEmployees(), "active headcount is still active-only");
        assertEquals(0, new BigDecimal("600.00").compareTo(stats.getStandardMealPrice()));
    }

    @Test
    void priceChange_doesNotRevalueHistory_andStoredAmountsAreUntouched() {
        try {
            settingsService.updateSetting("STANDARD_MEAL_PRICE", "700.00");
            DashboardStatsResponse stats = dashboardService.getDashboardStatistics();

            assertEquals(0, new BigDecimal("4200.00").compareTo(stats.getTotalExpenseCost()));
            assertEquals(0, new BigDecimal("700.00").compareTo(stats.getStandardMealPrice()));
            BigDecimal storedSum = mealRecordRepository.findAll().stream()
                    .map(MealRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, new BigDecimal("4200.00").compareTo(storedSum));
        } finally {
            // The DB change rolls back with the test transaction; also restore the in-memory cache.
            settingsService.updateSetting("STANDARD_MEAL_PRICE", "600.00");
        }
    }

    @Test
    void noAteRecords_givesRealZeros() {
        mealRecordRepository.deleteAll();
        DashboardStatsResponse stats = dashboardService.getDashboardStatistics();
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getTotalExpenseCost()));
        assertEquals(0, stats.getTotalMealsCount());
    }

    private Employee employee(String code, EmployeeStatus status) {
        return employeeRepository.save(Employee.builder()
                .employeeCode(code).firstName("Dash").lastName(code)
                .department("Production").position("Worker").phone("+2507889" + code.substring(2) + "00")
                .status(status).build());
    }

    private void record(Employee emp, LocalDate date, MealStatus status, String amount) {
        mealRecordRepository.save(MealRecord.builder()
                .employee(emp).mealDate(date).mealStatus(status)
                .amount(new BigDecimal(amount)).recordedBy("hr1").build());
    }
}
