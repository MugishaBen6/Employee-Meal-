package com.emeal.dto.excel;

import java.math.BigDecimal;

public class ExcelEmployeeRowDTO {

    private int rowNumber;
    private String employeeCode;
    private String employeeName;
    private String telephone;
    private String position;
    private String mealDate; // "YYYY-MM-DD" or formatted date
    private String mealStatus; // "Ate" or "Not Ate" / "ATE" / "DID_NOT_EAT"
    private BigDecimal amountUsed;
    private String status; // "VALID", "INVALID", "DUPLICATE"
    private String errorReason;

    public ExcelEmployeeRowDTO() {
    }

    public ExcelEmployeeRowDTO(int rowNumber, String employeeCode, String employeeName, String telephone, String position,
                               String mealDate, String mealStatus, BigDecimal amountUsed, String status, String errorReason) {
        this.rowNumber = rowNumber;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.telephone = telephone;
        this.position = position;
        this.mealDate = mealDate;
        this.mealStatus = mealStatus;
        this.amountUsed = amountUsed;
        this.status = status;
        this.errorReason = errorReason;
    }

    public static ExcelEmployeeRowDTOBuilder builder() {
        return new ExcelEmployeeRowDTOBuilder();
    }

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getMealDate() { return mealDate; }
    public void setMealDate(String mealDate) { this.mealDate = mealDate; }

    public String getMealStatus() { return mealStatus; }
    public void setMealStatus(String mealStatus) { this.mealStatus = mealStatus; }

    public BigDecimal getAmountUsed() { return amountUsed; }
    public void setAmountUsed(BigDecimal amountUsed) { this.amountUsed = amountUsed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isValid() { return "VALID".equalsIgnoreCase(status); }
    public boolean isDuplicate() { return "DUPLICATE".equalsIgnoreCase(status); }

    public String getErrorReason() { return errorReason; }
    public void setErrorReason(String errorReason) { this.errorReason = errorReason; }

    public static class ExcelEmployeeRowDTOBuilder {
        private int rowNumber;
        private String employeeCode;
        private String employeeName;
        private String telephone;
        private String position;
        private String mealDate;
        private String mealStatus;
        private BigDecimal amountUsed;
        private String status;
        private String errorReason;

        ExcelEmployeeRowDTOBuilder() {}

        public ExcelEmployeeRowDTOBuilder rowNumber(int rowNumber) { this.rowNumber = rowNumber; return this; }
        public ExcelEmployeeRowDTOBuilder employeeCode(String employeeCode) { this.employeeCode = employeeCode; return this; }
        public ExcelEmployeeRowDTOBuilder employeeName(String employeeName) { this.employeeName = employeeName; return this; }
        public ExcelEmployeeRowDTOBuilder telephone(String telephone) { this.telephone = telephone; return this; }
        public ExcelEmployeeRowDTOBuilder position(String position) { this.position = position; return this; }
        public ExcelEmployeeRowDTOBuilder mealDate(String mealDate) { this.mealDate = mealDate; return this; }
        public ExcelEmployeeRowDTOBuilder mealStatus(String mealStatus) { this.mealStatus = mealStatus; return this; }
        public ExcelEmployeeRowDTOBuilder amountUsed(BigDecimal amountUsed) { this.amountUsed = amountUsed; return this; }
        public ExcelEmployeeRowDTOBuilder status(String status) { this.status = status; return this; }
        public ExcelEmployeeRowDTOBuilder errorReason(String errorReason) { this.errorReason = errorReason; return this; }

        public ExcelEmployeeRowDTO build() {
            return new ExcelEmployeeRowDTO(rowNumber, employeeCode, employeeName, telephone, position, mealDate, mealStatus, amountUsed, status, errorReason);
        }
    }
}
