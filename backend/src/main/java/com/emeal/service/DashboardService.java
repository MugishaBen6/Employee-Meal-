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
        LocalDate latestDate = mealRecordRepository.findLatestMealDate();
        LocalDate activeDate = today;

        long todayAte = mealRecordRepository.countByMealDateAndMealStatus(today, MealStatus.ATE);
        long todayDidNotEat = mealRecordRepository.countByMealDateAndMealStatus(today, MealStatus.DID_NOT_EAT);

        if (todayAte == 0 && todayDidNotEat == 0 && latestDate != null) {
            activeDate = latestDate;
        }

        LocalDate startOfWeek = activeDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = activeDate.with(TemporalAdjusters.firstDayOfMonth());

        long totalActiveEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long ateToday = mealRecordRepository.countByMealDateAndMealStatus(activeDate, MealStatus.ATE);
        long didNotEatToday = mealRecordRepository.countByMealDateAndMealStatus(activeDate, MealStatus.DID_NOT_EAT);

        BigDecimal todayTotalCost = mealRecordRepository.sumAmountByMealDate(activeDate);
        BigDecimal thisWeekTotalCost = mealRecordRepository.sumAmountByMealDateBetween(startOfWeek, activeDate);
        BigDecimal thisMonthTotalCost = mealRecordRepository.sumAmountByMealDateBetween(startOfMonth, activeDate);

        BigDecimal averageMealCostToday = (ateToday > 0)
                ? todayTotalCost.divide(BigDecimal.valueOf(ateToday), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String currency = settingsService.getSettingValue("CURRENCY", "RWF");

        // 10 Days Chart Data covering the active range
        List<ExpenseChartData> dailyExpenditures = getExpendituresBetween(activeDate.minusDays(9), activeDate);

        // Department breakdown for activeDate
        List<DepartmentMealStats> departmentStats = getDepartmentStatsForDate(activeDate);

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
        List<Object[]> rows = mealRecordRepository.getDepartmentStatsForDate(date);
        List<DepartmentMealStats> stats = new ArrayList<>();

        for (Object[] row : rows) {
            String dept = (String) row[0];
            long totalEmployees = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long ateCount = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            long didNotEatCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            BigDecimal totalAmount = row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO;

            stats.add(DepartmentMealStats.builder()
                    .department(dept)
                    .totalEmployees(totalEmployees)
                    .ateCount(ateCount)
                    .didNotEatCount(didNotEatCount)
                    .totalAmount(totalAmount)
                    .build());
        }

        return stats;
    }
}
