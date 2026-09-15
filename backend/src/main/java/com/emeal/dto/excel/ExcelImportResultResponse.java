package com.emeal.dto.excel;

import java.util.List;

public class ExcelImportResultResponse {

    private int importedCount;
    private int duplicateCount;
    private int invalidCount;
    private List<ExcelEmployeeRowDTO> failedRows;

    public ExcelImportResultResponse() {
    }

    public ExcelImportResultResponse(int importedCount, int duplicateCount, int invalidCount, List<ExcelEmployeeRowDTO> failedRows) {
        this.importedCount = importedCount;
        this.duplicateCount = duplicateCount;
        this.invalidCount = invalidCount;
        this.failedRows = failedRows;
    }

    public static ExcelImportResultResponseBuilder builder() {
        return new ExcelImportResultResponseBuilder();
    }

    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }

    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }

    public int getInvalidCount() { return invalidCount; }
    public void setInvalidCount(int invalidCount) { this.invalidCount = invalidCount; }

    public List<ExcelEmployeeRowDTO> getFailedRows() { return failedRows; }
    public void setFailedRows(List<ExcelEmployeeRowDTO> failedRows) { this.failedRows = failedRows; }

    public static class ExcelImportResultResponseBuilder {
        private int importedCount;
        private int duplicateCount;
        private int invalidCount;
        private List<ExcelEmployeeRowDTO> failedRows;

        ExcelImportResultResponseBuilder() {}

        public ExcelImportResultResponseBuilder importedCount(int importedCount) { this.importedCount = importedCount; return this; }
        public ExcelImportResultResponseBuilder duplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; return this; }
        public ExcelImportResultResponseBuilder invalidCount(int invalidCount) { this.invalidCount = invalidCount; return this; }
        public ExcelImportResultResponseBuilder failedRows(List<ExcelEmployeeRowDTO> failedRows) { this.failedRows = failedRows; return this; }

        public ExcelImportResultResponse build() {
            return new ExcelImportResultResponse(importedCount, duplicateCount, invalidCount, failedRows);
        }
    }
}
