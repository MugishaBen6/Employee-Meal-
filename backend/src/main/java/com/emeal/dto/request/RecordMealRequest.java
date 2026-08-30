package com.emeal.dto.request;

import com.emeal.entity.MealStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecordMealRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Meal date is required")
    private LocalDate mealDate;

    private MealStatus mealStatus = MealStatus.ATE;

    @DecimalMin(value = "0.00", message = "Amount must be greater than or equal to 0")
    private BigDecimal amount;

    public RecordMealRequest() {
    }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public LocalDate getMealDate() { return mealDate; }
    public void setMealDate(LocalDate mealDate) { this.mealDate = mealDate; }

    public MealStatus getMealStatus() { return mealStatus; }
    public void setMealStatus(MealStatus mealStatus) { this.mealStatus = mealStatus; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
