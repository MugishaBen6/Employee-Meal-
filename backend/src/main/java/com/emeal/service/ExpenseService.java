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
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());

        BigDecimal todayExpense = mealRecordRepository.sumAmountByMealDate(today);
        BigDecimal weeklyExpense = mealRecordRepository.sumAmountByMealDateBetween(startOfWeek, today);
        BigDecimal monthlyExpense = mealRecordRepository.sumAmountByMealDateBetween(startOfMonth, today);
        BigDecimal totalExpense = mealRecordRepository.sumAmountByMealDateBetween(LocalDate.of(2000, 1, 1), today);

        List<ExpenseChartData> last30Days = dashboardService.getExpendituresBetween(today.minusDays(29), today);

        Map<String, Object> res = new HashMap<>();
        res.put("todayExpense", todayExpense);
        res.put("weeklyExpense", weeklyExpense);
        res.put("monthlyExpense", monthlyExpense);
        res.put("totalExpense", totalExpense);
        res.put("chartData", last30Days);

        return res;
    }
}
