package com.emeal.dto.response;

import java.math.BigDecimal;

public class DepartmentMealStats {

    private String department;
    private long totalEmployees;
    private long ateCount;
    private long didNotEatCount;
    private BigDecimal totalAmount;

    public DepartmentMealStats() {
    }

    public DepartmentMealStats(String department, long totalEmployees, long ateCount, long didNotEatCount, BigDecimal totalAmount) {
        this.department = department;
        this.totalEmployees = totalEmployees;
        this.ateCount = ateCount;
        this.didNotEatCount = didNotEatCount;
        this.totalAmount = totalAmount;
    }

    public static DepartmentMealStatsBuilder builder() {
        return new DepartmentMealStatsBuilder();
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public long getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; }

    public long getAteCount() { return ateCount; }
    public void setAteCount(long ateCount) { this.ateCount = ateCount; }

    public long getDidNotEatCount() { return didNotEatCount; }
    public void setDidNotEatCount(long didNotEatCount) { this.didNotEatCount = didNotEatCount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public static class DepartmentMealStatsBuilder {
        private String department;
        private long totalEmployees;
        private long ateCount;
        private long didNotEatCount;
        private BigDecimal totalAmount;

        DepartmentMealStatsBuilder() {}

        public DepartmentMealStatsBuilder department(String department) { this.department = department; return this; }
        public DepartmentMealStatsBuilder totalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; return this; }
        public DepartmentMealStatsBuilder ateCount(long ateCount) { this.ateCount = ateCount; return this; }
        public DepartmentMealStatsBuilder didNotEatCount(long didNotEatCount) { this.didNotEatCount = didNotEatCount; return this; }
        public DepartmentMealStatsBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }

        public DepartmentMealStats build() {
            return new DepartmentMealStats(department, totalEmployees, ateCount, didNotEatCount, totalAmount);
        }
    }
}
