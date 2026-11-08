package com.emeal.dto.request;

import com.emeal.entity.MealStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdateMealRecordRequest {

    @NotNull(message = "Meal status is required")
    private MealStatus mealStatus;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", message = "Amount must be greater than or equal to 0")
    private BigDecimal amount;

    /** Optional: move the record to another meal date (never changed unless sent). */
    private java.time.LocalDate mealDate;

    /** Optional: assign the record to a different (correct) employee. */
    private Long employeeId;

    /** Optional: correct the employee's name (changes the employee itself, all their records). */
    private String employeeName;

    /** Optional: correct the employee's position. */
    private String position;

    public UpdateMealRecordRequest() {
    }

    public java.time.LocalDate getMealDate() { return mealDate; }
    public void setMealDate(java.time.LocalDate mealDate) { this.mealDate = mealDate; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public MealStatus getMealStatus() { return mealStatus; }
    public void setMealStatus(MealStatus mealStatus) { this.mealStatus = mealStatus; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
