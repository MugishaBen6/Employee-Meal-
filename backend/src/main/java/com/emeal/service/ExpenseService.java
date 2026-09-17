package com.emeal.service;

import com.emeal.dto.response.ExpenseChartData;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseService {

    private final MealRecordRepository mealRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final DashboardService dashboardService;

    public ExpenseService(MealRecordRepository mealRecordRepository, EmployeeRepository employeeRepository, DashboardService dashboardService) {
        this.mealRecordRepository = mealRecordRepository;
        this.employeeRepository = employeeRepository;
        this.dashboardService = dashboardService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getExpenseSummary() {
        LocalDate today = LocalDate.now();
        LocalDate latestDate = mealRecordRepository.findLatestMealDate();
        LocalDate activeDate = (latestDate != null) ? latestDate : today;

        LocalDate startOfWeek = activeDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = activeDate.with(TemporalAdjusters.firstDayOfMonth());

        BigDecimal todayExpense = mealRecordRepository.sumAmountByMealDate(activeDate);
        BigDecimal weeklyExpense = mealRecordRepository.sumAmountByMealDateBetween(startOfWeek, activeDate);
        BigDecimal monthlyExpense = mealRecordRepository.sumAmountByMealDateBetween(startOfMonth, activeDate);
        BigDecimal totalExpense = mealRecordRepository.sumTotalAmount();

        List<ExpenseChartData> last30Days = dashboardService.getExpendituresBetween(activeDate.minusDays(29), activeDate);

        Map<String, Object> res = new HashMap<>();
        res.put("todayExpense", todayExpense);
        res.put("weeklyExpense", weeklyExpense);
        res.put("monthlyExpense", monthlyExpense);
        res.put("totalExpense", totalExpense);
        res.put("chartData", last30Days);

        return res;
    }
}
