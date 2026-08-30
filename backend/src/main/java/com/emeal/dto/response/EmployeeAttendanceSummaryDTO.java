package com.emeal.dto.response;

import java.math.BigDecimal;

public class EmployeeAttendanceSummaryDTO {

    private long totalActiveEmployees;
    private long ateCount;
    private long didNotEatCount;
    private long notRecordedCount;
    private BigDecimal totalMealCost;
    private String currency;

    public EmployeeAttendanceSummaryDTO() {
    }

    public EmployeeAttendanceSummaryDTO(long totalActiveEmployees, long ateCount, long didNotEatCount,
                                        long notRecordedCount, BigDecimal totalMealCost, String currency) {
        this.totalActiveEmployees = totalActiveEmployees;
        this.ateCount = ateCount;
        this.didNotEatCount = didNotEatCount;
        this.notRecordedCount = notRecordedCount;
        this.totalMealCost = totalMealCost;
        this.currency = currency;
    }

    public static EmployeeAttendanceSummaryDTOBuilder builder() {
        return new EmployeeAttendanceSummaryDTOBuilder();
    }

    public long getTotalActiveEmployees() { return totalActiveEmployees; }
    public void setTotalActiveEmployees(long totalActiveEmployees) { this.totalActiveEmployees = totalActiveEmployees; }

    public long getAteCount() { return ateCount; }
    public void setAteCount(long ateCount) { this.ateCount = ateCount; }

    public long getDidNotEatCount() { return didNotEatCount; }
    public void setDidNotEatCount(long didNotEatCount) { this.didNotEatCount = didNotEatCount; }

    public long getNotRecordedCount() { return notRecordedCount; }
    public void setNotRecordedCount(long notRecordedCount) { this.notRecordedCount = notRecordedCount; }

    public BigDecimal getTotalMealCost() { return totalMealCost; }
    public void setTotalMealCost(BigDecimal totalMealCost) { this.totalMealCost = totalMealCost; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public static class EmployeeAttendanceSummaryDTOBuilder {
        private long totalActiveEmployees;
        private long ateCount;
        private long didNotEatCount;
        private long notRecordedCount;
        private BigDecimal totalMealCost;
        private String currency;

        EmployeeAttendanceSummaryDTOBuilder() {}

        public EmployeeAttendanceSummaryDTOBuilder totalActiveEmployees(long totalActiveEmployees) {
            this.totalActiveEmployees = totalActiveEmployees;
            return this;
        }

        public EmployeeAttendanceSummaryDTOBuilder ateCount(long ateCount) {
            this.ateCount = ateCount;
            return this;
        }

        public EmployeeAttendanceSummaryDTOBuilder didNotEatCount(long didNotEatCount) {
            this.didNotEatCount = didNotEatCount;
            return this;
        }

        public EmployeeAttendanceSummaryDTOBuilder notRecordedCount(long notRecordedCount) {
            this.notRecordedCount = notRecordedCount;
            return this;
        }

        public EmployeeAttendanceSummaryDTOBuilder totalMealCost(BigDecimal totalMealCost) {
            this.totalMealCost = totalMealCost;
            return this;
        }

        public EmployeeAttendanceSummaryDTOBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public EmployeeAttendanceSummaryDTO build() {
            return new EmployeeAttendanceSummaryDTO(totalActiveEmployees, ateCount, didNotEatCount,
                    notRecordedCount, totalMealCost, currency);
        }
    }
}
