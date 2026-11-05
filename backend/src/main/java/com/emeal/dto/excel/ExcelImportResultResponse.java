package com.emeal.dto.excel;

import java.util.List;

public class ExcelImportResultResponse {

    private int importedCount;
    private int employeesProcessed;
    private int mealRecordsCreated;
    private int ateCount;
    private int didNotEatCount;
    private int duplicateCount;
    private int invalidCount;
    private String message;
    private List<ExcelEmployeeRowDTO> failedRows;

    public ExcelImportResultResponse() {
    }

    public ExcelImportResultResponse(int importedCount, int employeesProcessed, int mealRecordsCreated, int ateCount, int didNotEatCount, int duplicateCount, int invalidCount, String message, List<ExcelEmployeeRowDTO> failedRows) {
        this.importedCount = importedCount;
        this.employeesProcessed = employeesProcessed;
        this.mealRecordsCreated = mealRecordsCreated;
        this.ateCount = ateCount;
        this.didNotEatCount = didNotEatCount;
        this.duplicateCount = duplicateCount;
        this.invalidCount = invalidCount;
        this.message = message;
        this.failedRows = failedRows;
    }


    // ---- Import summary (real figures computed from the parsed file) ----
    private int ateRecords;
    private int noMealCells;
    private int newEmployees;
    private String firstMealDate;
    private String lastMealDate;
    private java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
    private List<String> months = new java.util.ArrayList<>();
    private List<String> warnings = new java.util.ArrayList<>();

    public int getAteRecords() { return ateRecords; }
    public void setAteRecords(int ateRecords) { this.ateRecords = ateRecords; }
    public int getNoMealCells() { return noMealCells; }
    public void setNoMealCells(int noMealCells) { this.noMealCells = noMealCells; }
    public int getNewEmployees() { return newEmployees; }
    public void setNewEmployees(int newEmployees) { this.newEmployees = newEmployees; }
    public String getFirstMealDate() { return firstMealDate; }
    public void setFirstMealDate(String firstMealDate) { this.firstMealDate = firstMealDate; }
    public String getLastMealDate() { return lastMealDate; }
    public void setLastMealDate(String lastMealDate) { this.lastMealDate = lastMealDate; }
    public java.math.BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public List<String> getMonths() { return months; }
    public void setMonths(List<String> months) { this.months = months; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    private int recordsCreated;
    private int recordsUpdated;
    public int getRecordsCreated() { return recordsCreated; }
    public void setRecordsCreated(int recordsCreated) { this.recordsCreated = recordsCreated; }
    public int getRecordsUpdated() { return recordsUpdated; }
    public void setRecordsUpdated(int recordsUpdated) { this.recordsUpdated = recordsUpdated; }

    public static ExcelImportResultResponseBuilder builder() {
        return new ExcelImportResultResponseBuilder();
    }

    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }

    public int getEmployeesProcessed() { return employeesProcessed; }
    public void setEmployeesProcessed(int employeesProcessed) { this.employeesProcessed = employeesProcessed; }

    public int getMealRecordsCreated() { return mealRecordsCreated; }
    public void setMealRecordsCreated(int mealRecordsCreated) { this.mealRecordsCreated = mealRecordsCreated; }

    public int getAteCount() { return ateCount; }
    public void setAteCount(int ateCount) { this.ateCount = ateCount; }

    public int getDidNotEatCount() { return didNotEatCount; }
    public void setDidNotEatCount(int didNotEatCount) { this.didNotEatCount = didNotEatCount; }

    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }

    public int getInvalidCount() { return invalidCount; }
    public void setInvalidCount(int invalidCount) { this.invalidCount = invalidCount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<ExcelEmployeeRowDTO> getFailedRows() { return failedRows; }
    public void setFailedRows(List<ExcelEmployeeRowDTO> failedRows) { this.failedRows = failedRows; }

    public static class ExcelImportResultResponseBuilder {
        private int importedCount;
        private int employeesProcessed;
        private int mealRecordsCreated;
        private int ateCount;
        private int didNotEatCount;
        private int duplicateCount;
        private int invalidCount;
        private String message;
        private List<ExcelEmployeeRowDTO> failedRows;

        ExcelImportResultResponseBuilder() {}

        public ExcelImportResultResponseBuilder importedCount(int importedCount) { this.importedCount = importedCount; return this; }
        public ExcelImportResultResponseBuilder employeesProcessed(int employeesProcessed) { this.employeesProcessed = employeesProcessed; return this; }
        public ExcelImportResultResponseBuilder mealRecordsCreated(int mealRecordsCreated) { this.mealRecordsCreated = mealRecordsCreated; return this; }
        public ExcelImportResultResponseBuilder ateCount(int ateCount) { this.ateCount = ateCount; return this; }
        public ExcelImportResultResponseBuilder didNotEatCount(int didNotEatCount) { this.didNotEatCount = didNotEatCount; return this; }
        public ExcelImportResultResponseBuilder duplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; return this; }
        public ExcelImportResultResponseBuilder invalidCount(int invalidCount) { this.invalidCount = invalidCount; return this; }
        public ExcelImportResultResponseBuilder message(String message) { this.message = message; return this; }
        public ExcelImportResultResponseBuilder failedRows(List<ExcelEmployeeRowDTO> failedRows) { this.failedRows = failedRows; return this; }

        public ExcelImportResultResponse build() {
            return new ExcelImportResultResponse(importedCount, employeesProcessed, mealRecordsCreated, ateCount, didNotEatCount, duplicateCount, invalidCount, message, failedRows);
        }
    }
}
