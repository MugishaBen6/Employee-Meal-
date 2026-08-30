package com.emeal.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class DashboardStatsResponse {

    private long totalEmployees;
    private long ateToday;
    private long didNotEatToday;
    private BigDecimal todayTotalCost;
    private BigDecimal thisWeekTotalCost;
    private BigDecimal thisMonthTotalCost;
    private BigDecimal averageMealCostToday;
    private String currency;
    
    private List<ExpenseChartData> dailyExpenditures;
    private List<DepartmentMealStats> departmentStats;
    private List<AuditLogDTO> recentActivities;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(long totalEmployees, long ateToday, long didNotEatToday, BigDecimal todayTotalCost, BigDecimal thisWeekTotalCost, BigDecimal thisMonthTotalCost, BigDecimal averageMealCostToday, String currency, List<ExpenseChartData> dailyExpenditures, List<DepartmentMealStats> departmentStats, List<AuditLogDTO> recentActivities) {
        this.totalEmployees = totalEmployees;
        this.ateToday = ateToday;
        this.didNotEatToday = didNotEatToday;
        this.todayTotalCost = todayTotalCost;
        this.thisWeekTotalCost = thisWeekTotalCost;
        this.thisMonthTotalCost = thisMonthTotalCost;
        this.averageMealCostToday = averageMealCostToday;
        this.currency = currency;
        this.dailyExpenditures = dailyExpenditures;
        this.departmentStats = departmentStats;
        this.recentActivities = recentActivities;
    }

    public static DashboardStatsResponseBuilder builder() {
        return new DashboardStatsResponseBuilder();
    }

    public long getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; }

    public long getAteToday() { return ateToday; }
    public void setAteToday(long ateToday) { this.ateToday = ateToday; }

    public long getDidNotEatToday() { return didNotEatToday; }
    public void setDidNotEatToday(long didNotEatToday) { this.didNotEatToday = didNotEatToday; }

    public BigDecimal getTodayTotalCost() { return todayTotalCost; }
    public void setTodayTotalCost(BigDecimal todayTotalCost) { this.todayTotalCost = todayTotalCost; }

    public BigDecimal getThisWeekTotalCost() { return thisWeekTotalCost; }
    public void setThisWeekTotalCost(BigDecimal thisWeekTotalCost) { this.thisWeekTotalCost = thisWeekTotalCost; }

    public BigDecimal getThisMonthTotalCost() { return thisMonthTotalCost; }
    public void setThisMonthTotalCost(BigDecimal thisMonthTotalCost) { this.thisMonthTotalCost = thisMonthTotalCost; }

    public BigDecimal getAverageMealCostToday() { return averageMealCostToday; }
    public void setAverageMealCostToday(BigDecimal averageMealCostToday) { this.averageMealCostToday = averageMealCostToday; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public List<ExpenseChartData> getDailyExpenditures() { return dailyExpenditures; }
    public void setDailyExpenditures(List<ExpenseChartData> dailyExpenditures) { this.dailyExpenditures = dailyExpenditures; }

    public List<DepartmentMealStats> getDepartmentStats() { return departmentStats; }
    public void setDepartmentStats(List<DepartmentMealStats> departmentStats) { this.departmentStats = departmentStats; }

    public List<AuditLogDTO> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<AuditLogDTO> recentActivities) { this.recentActivities = recentActivities; }

    public static class DashboardStatsResponseBuilder {
        private long totalEmployees;
        private long ateToday;
        private long didNotEatToday;
        private BigDecimal todayTotalCost;
        private BigDecimal thisWeekTotalCost;
        private BigDecimal thisMonthTotalCost;
        private BigDecimal averageMealCostToday;
        private String currency;
        private List<ExpenseChartData> dailyExpenditures;
        private List<DepartmentMealStats> departmentStats;
        private List<AuditLogDTO> recentActivities;

        DashboardStatsResponseBuilder() {}

        public DashboardStatsResponseBuilder totalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; return this; }
        public DashboardStatsResponseBuilder ateToday(long ateToday) { this.ateToday = ateToday; return this; }
        public DashboardStatsResponseBuilder didNotEatToday(long didNotEatToday) { this.didNotEatToday = didNotEatToday; return this; }
        public DashboardStatsResponseBuilder todayTotalCost(BigDecimal todayTotalCost) { this.todayTotalCost = todayTotalCost; return this; }
        public DashboardStatsResponseBuilder thisWeekTotalCost(BigDecimal thisWeekTotalCost) { this.thisWeekTotalCost = thisWeekTotalCost; return this; }
        public DashboardStatsResponseBuilder thisMonthTotalCost(BigDecimal thisMonthTotalCost) { this.thisMonthTotalCost = thisMonthTotalCost; return this; }
        public DashboardStatsResponseBuilder averageMealCostToday(BigDecimal averageMealCostToday) { this.averageMealCostToday = averageMealCostToday; return this; }
        public DashboardStatsResponseBuilder currency(String currency) { this.currency = currency; return this; }
        public DashboardStatsResponseBuilder dailyExpenditures(List<ExpenseChartData> dailyExpenditures) { this.dailyExpenditures = dailyExpenditures; return this; }
        public DashboardStatsResponseBuilder departmentStats(List<DepartmentMealStats> departmentStats) { this.departmentStats = departmentStats; return this; }
        public DashboardStatsResponseBuilder recentActivities(List<AuditLogDTO> recentActivities) { this.recentActivities = recentActivities; return this; }

        public DashboardStatsResponse build() {
            return new DashboardStatsResponse(totalEmployees, ateToday, didNotEatToday, todayTotalCost, thisWeekTotalCost, thisMonthTotalCost, averageMealCostToday, currency, dailyExpenditures, departmentStats, recentActivities);
        }
    }
}
