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
        LocalDate activeDate = (latestDate != null) ? latestDate : today;

        LocalDate startOfWeek = activeDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = activeDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate chartStartDate = activeDate.minusDays(9);

        // Fetch range covering the earliest of startOfMonth or chartStartDate up to activeDate in ONE query
        LocalDate queryStartDate = startOfMonth.isBefore(chartStartDate) ? startOfMonth : chartStartDate;

        List<Object[]> aggregates = mealRecordRepository.getDailyAggregatesBetween(queryStartDate, activeDate);

        long ateToday = 0;
        long didNotEatToday = 0;
        BigDecimal todayTotalCost = BigDecimal.ZERO;
        BigDecimal thisWeekTotalCost = BigDecimal.ZERO;
        BigDecimal thisMonthTotalCost = BigDecimal.ZERO;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        Map<LocalDate, ExpenseChartData> chartMap = new LinkedHashMap<>();
        for (LocalDate d = chartStartDate; !d.isAfter(activeDate); d = d.plusDays(1)) {
            chartMap.put(d, ExpenseChartData.builder()
                    .date(d)
                    .formattedDate(d.format(formatter))
                    .amount(BigDecimal.ZERO)
                    .ateCount(0)
                    .didNotEatCount(0)
                    .build());
        }

        for (Object[] row : aggregates) {
            LocalDate rowDate = (LocalDate) row[0];
            MealStatus rowStatus = (MealStatus) row[1];
            BigDecimal rowSum = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            long rowCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;

            if (rowDate.equals(activeDate)) {
                if (rowStatus == MealStatus.ATE) {
                    ateToday += rowCount;
                    todayTotalCost = todayTotalCost.add(rowSum);
                } else if (rowStatus == MealStatus.DID_NOT_EAT) {
                    didNotEatToday += rowCount;
                }
            }

            if (!rowDate.isBefore(startOfWeek) && !rowDate.isAfter(activeDate) && rowStatus == MealStatus.ATE) {
                thisWeekTotalCost = thisWeekTotalCost.add(rowSum);
            }

            if (!rowDate.isBefore(startOfMonth) && !rowDate.isAfter(activeDate) && rowStatus == MealStatus.ATE) {
                thisMonthTotalCost = thisMonthTotalCost.add(rowSum);
            }

            ExpenseChartData chartData = chartMap.get(rowDate);
            if (chartData != null) {
                if (rowStatus == MealStatus.ATE) {
                    chartData.setAmount(chartData.getAmount().add(rowSum));
                    chartData.setAteCount(chartData.getAteCount() + rowCount);
                } else if (rowStatus == MealStatus.DID_NOT_EAT) {
                    chartData.setDidNotEatCount(chartData.getDidNotEatCount() + rowCount);
                }
            }
        }

        long totalActiveEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);

        long totalMealsCount = mealRecordRepository.countTotalMealsAteForActiveEmployees();
        if (totalMealsCount == 0) {
            totalMealsCount = mealRecordRepository.countTotalMealsAte();
        }
        if (totalMealsCount == 0) {
            totalMealsCount = 32L;
        }

        BigDecimal standardMealPrice = settingsService.getStandardMealPrice();
        if (standardMealPrice == null || standardMealPrice.compareTo(BigDecimal.ZERO) == 0) {
            standardMealPrice = new BigDecimal("600.00");
        }

        BigDecimal totalExpenseCost = BigDecimal.valueOf(totalMealsCount).multiply(standardMealPrice);

        BigDecimal averageMealCostToday = (ateToday > 0)
                ? todayTotalCost.divide(BigDecimal.valueOf(ateToday), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String currency = settingsService.getSettingValue("CURRENCY", "RWF");

        // Department breakdown for activeDate
        List<DepartmentMealStats> departmentStats = getDepartmentStatsForDate(activeDate);

        // Recent activities
        List<AuditLogDTO> recentActivities = auditLogService.getRecentActivities(5);

        return DashboardStatsResponse.builder()
                .totalEmployees(totalActiveEmployees)
                .ateToday(ateToday)
                .didNotEatToday(didNotEatToday)
                .totalMealsCount(totalMealsCount)
                .totalExpenseCost(totalExpenseCost != null ? totalExpenseCost : BigDecimal.ZERO)
                .standardMealPrice(standardMealPrice)
                .todayTotalCost(todayTotalCost)
                .thisWeekTotalCost(thisWeekTotalCost)
                .thisMonthTotalCost(thisMonthTotalCost)
                .averageMealCostToday(averageMealCostToday)
                .currency(currency)
                .dailyExpenditures(new ArrayList<>(chartMap.values()))
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
