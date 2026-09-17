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
