package com.emeal.service;

import com.emeal.dto.response.AuditLogDTO;
import com.emeal.dto.response.DashboardStatsResponse;
import com.emeal.dto.response.DepartmentMealStats;
import com.emeal.dto.response.ExpenseChartData;
import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final MealRecordRepository mealRecordRepository;
    private final SettingsService settingsService;
    private final AuditLogService auditLogService;

    public DashboardService(EmployeeRepository employeeRepository, MealRecordRepository mealRecordRepository, SettingsService settingsService, AuditLogService auditLogService) {
        this.employeeRepository = employeeRepository;
        this.mealRecordRepository = mealRecordRepository;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStatistics() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());

        long totalActiveEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long ateToday = mealRecordRepository.countByMealDateAndMealStatus(today, MealStatus.ATE);
        long didNotEatToday = mealRecordRepository.countByMealDateAndMealStatus(today, MealStatus.DID_NOT_EAT);

        BigDecimal todayTotalCost = mealRecordRepository.sumAmountByMealDate(today);
        BigDecimal thisWeekTotalCost = mealRecordRepository.sumAmountByMealDateBetween(startOfWeek, today);
        BigDecimal thisMonthTotalCost = mealRecordRepository.sumAmountByMealDateBetween(startOfMonth, today);

        BigDecimal averageMealCostToday = (ateToday > 0)
                ? todayTotalCost.divide(BigDecimal.valueOf(ateToday), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String currency = settingsService.getSettingValue("CURRENCY", "RWF");

        // 7 Days Chart Data
        List<ExpenseChartData> dailyExpenditures = getExpendituresBetween(today.minusDays(6), today);

        // Department breakdown
        List<DepartmentMealStats> departmentStats = getDepartmentStatsForDate(today);

        // Recent activities
        List<AuditLogDTO> recentActivities = auditLogService.getRecentActivities(5);

        return DashboardStatsResponse.builder()
                .totalEmployees(totalActiveEmployees)
                .ateToday(ateToday)
                .didNotEatToday(didNotEatToday)
                .todayTotalCost(todayTotalCost)
                .thisWeekTotalCost(thisWeekTotalCost)
                .thisMonthTotalCost(thisMonthTotalCost)
                .averageMealCostToday(averageMealCostToday)
                .currency(currency)
                .dailyExpenditures(dailyExpenditures)
                .departmentStats(departmentStats)
                .recentActivities(recentActivities)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ExpenseChartData> getExpendituresBetween(LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        Map<LocalDate, ExpenseChartData> dateMap = new LinkedHashMap<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dateMap.put(date, ExpenseChartData.builder()
                    .date(date)
                    .formattedDate(date.format(formatter))
                    .amount(BigDecimal.ZERO)
                    .ateCount(0)
                    .didNotEatCount(0)
                    .build());
        }

        List<Object[]> aggregates = mealRecordRepository.getDailyAggregatesBetween(startDate, endDate);
        for (Object[] row : aggregates) {
            LocalDate date = (LocalDate) row[0];
            MealStatus status = (MealStatus) row[1];
            BigDecimal sum = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            long count = row[3] != null ? ((Number) row[3]).longValue() : 0L;

            ExpenseChartData data = dateMap.get(date);
            if (data != null) {
                if (status == MealStatus.ATE) {
                    data.setAmount(data.getAmount().add(sum));
                    data.setAteCount(data.getAteCount() + count);
                } else if (status == MealStatus.DID_NOT_EAT) {
                    data.setDidNotEatCount(data.getDidNotEatCount() + count);
                }
            }
        }

        return new ArrayList<>(dateMap.values());
    }

    @Transactional(readOnly = true)
    public List<DepartmentMealStats> getDepartmentStatsForDate(LocalDate date) {
        List<Employee> employees = employeeRepository.findAll();
        Map<String, List<Employee>> deptMap = employees.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment));

        List<MealRecord> records = mealRecordRepository.findByMealDate(date);
        Map<Long, MealRecord> recordMap = records.stream()
                .collect(Collectors.toMap(r -> r.getEmployee().getId(), r -> r, (r1, r2) -> r1));

        List<DepartmentMealStats> stats = new ArrayList<>();

        for (Map.Entry<String, List<Employee>> entry : deptMap.entrySet()) {
            String dept = entry.getKey();
            List<Employee> deptEmployees = entry.getValue();
            long total = deptEmployees.size();
            long ate = 0;
            long didNotEat = 0;
            BigDecimal amount = BigDecimal.ZERO;

            for (Employee emp : deptEmployees) {
                MealRecord mr = recordMap.get(emp.getId());
                if (mr != null) {
                    if (mr.getMealStatus() == MealStatus.ATE) {
                        ate++;
                        amount = amount.add(mr.getAmount());
                    } else if (mr.getMealStatus() == MealStatus.DID_NOT_EAT) {
                        didNotEat++;
                    }
                }
            }

            stats.add(DepartmentMealStats.builder()
                    .department(dept)
                    .totalEmployees(total)
                    .ateCount(ate)
                    .didNotEatCount(didNotEat)
                    .totalAmount(amount)
                    .build());
        }

        stats.sort(Comparator.comparing(DepartmentMealStats::getDepartment));
        return stats;
    }
}
