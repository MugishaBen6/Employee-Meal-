package com.emeal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class QuickMealCheckResponse {

    private EmployeeDTO employee;
    private boolean alreadyRecordedToday;
    private MealRecordDTO todayRecord;
    private BigDecimal defaultMealPrice;
    private LocalDate targetDate;

    public QuickMealCheckResponse() {
    }

    public QuickMealCheckResponse(EmployeeDTO employee, boolean alreadyRecordedToday, MealRecordDTO todayRecord, BigDecimal defaultMealPrice, LocalDate targetDate) {
        this.employee = employee;
        this.alreadyRecordedToday = alreadyRecordedToday;
        this.todayRecord = todayRecord;
        this.defaultMealPrice = defaultMealPrice;
        this.targetDate = targetDate;
    }

    public static QuickMealCheckResponseBuilder builder() {
        return new QuickMealCheckResponseBuilder();
    }

    public EmployeeDTO getEmployee() { return employee; }
    public void setEmployee(EmployeeDTO employee) { this.employee = employee; }

    public boolean isAlreadyRecordedToday() { return alreadyRecordedToday; }
    public void setAlreadyRecordedToday(boolean alreadyRecordedToday) { this.alreadyRecordedToday = alreadyRecordedToday; }

    public MealRecordDTO getTodayRecord() { return todayRecord; }
    public void setTodayRecord(MealRecordDTO todayRecord) { this.todayRecord = todayRecord; }

    public BigDecimal getDefaultMealPrice() { return defaultMealPrice; }
    public void setDefaultMealPrice(BigDecimal defaultMealPrice) { this.defaultMealPrice = defaultMealPrice; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public static class QuickMealCheckResponseBuilder {
        private EmployeeDTO employee;
        private boolean alreadyRecordedToday;
        private MealRecordDTO todayRecord;
        private BigDecimal defaultMealPrice;
        private LocalDate targetDate;

        QuickMealCheckResponseBuilder() {}

        public QuickMealCheckResponseBuilder employee(EmployeeDTO employee) { this.employee = employee; return this; }
        public QuickMealCheckResponseBuilder alreadyRecordedToday(boolean alreadyRecordedToday) { this.alreadyRecordedToday = alreadyRecordedToday; return this; }
        public QuickMealCheckResponseBuilder todayRecord(MealRecordDTO todayRecord) { this.todayRecord = todayRecord; return this; }
        public QuickMealCheckResponseBuilder defaultMealPrice(BigDecimal defaultMealPrice) { this.defaultMealPrice = defaultMealPrice; return this; }
        public QuickMealCheckResponseBuilder targetDate(LocalDate targetDate) { this.targetDate = targetDate; return this; }

        public QuickMealCheckResponse build() {
            return new QuickMealCheckResponse(employee, alreadyRecordedToday, todayRecord, defaultMealPrice, targetDate);
        }
    }
}
