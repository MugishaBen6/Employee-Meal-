package com.emeal.dto.excel;

import java.util.List;

public class ExcelImportPreviewResponse {

    private int totalRows;
    private int validRows;
    private int invalidRows;
    private int duplicateRows;
    private List<ExcelEmployeeRowDTO> rows;

    public ExcelImportPreviewResponse() {
    }

    public ExcelImportPreviewResponse(int totalRows, int validRows, int invalidRows, int duplicateRows, List<ExcelEmployeeRowDTO> rows) {
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.duplicateRows = duplicateRows;
        this.rows = rows;
    }

    public static ExcelImportPreviewResponseBuilder builder() {
        return new ExcelImportPreviewResponseBuilder();
    }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getValidRows() { return validRows; }
    public void setValidRows(int validRows) { this.validRows = validRows; }

    public int getInvalidRows() { return invalidRows; }
    public void setInvalidRows(int invalidRows) { this.invalidRows = invalidRows; }

    public int getDuplicateRows() { return duplicateRows; }
    public void setDuplicateRows(int duplicateRows) { this.duplicateRows = duplicateRows; }

    public List<ExcelEmployeeRowDTO> getRows() { return rows; }
    public void setRows(List<ExcelEmployeeRowDTO> rows) { this.rows = rows; }

    public static class ExcelImportPreviewResponseBuilder {
        private int totalRows;
        private int validRows;
        private int invalidRows;
        private int duplicateRows;
        private List<ExcelEmployeeRowDTO> rows;

        ExcelImportPreviewResponseBuilder() {}

        public ExcelImportPreviewResponseBuilder totalRows(int totalRows) { this.totalRows = totalRows; return this; }
        public ExcelImportPreviewResponseBuilder validRows(int validRows) { this.validRows = validRows; return this; }
        public ExcelImportPreviewResponseBuilder invalidRows(int invalidRows) { this.invalidRows = invalidRows; return this; }
        public ExcelImportPreviewResponseBuilder duplicateRows(int duplicateRows) { this.duplicateRows = duplicateRows; return this; }
        public ExcelImportPreviewResponseBuilder rows(List<ExcelEmployeeRowDTO> rows) { this.rows = rows; return this; }

        public ExcelImportPreviewResponse build() {
            return new ExcelImportPreviewResponse(totalRows, validRows, invalidRows, duplicateRows, rows);
        }
    }
}
