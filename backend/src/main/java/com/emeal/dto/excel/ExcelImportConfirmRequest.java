package com.emeal.dto.excel;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public class ExcelImportConfirmRequest {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate mealDate;

    private List<ExcelEmployeeRowDTO> rows;

    public ExcelImportConfirmRequest() {
    }

    public ExcelImportConfirmRequest(LocalDate mealDate, List<ExcelEmployeeRowDTO> rows) {
        this.mealDate = mealDate;
        this.rows = rows;
    }

    public LocalDate getMealDate() { return mealDate; }
    public void setMealDate(LocalDate mealDate) { this.mealDate = mealDate; }

    public List<ExcelEmployeeRowDTO> getRows() { return rows; }
    public void setRows(List<ExcelEmployeeRowDTO> rows) { this.rows = rows; }
}
