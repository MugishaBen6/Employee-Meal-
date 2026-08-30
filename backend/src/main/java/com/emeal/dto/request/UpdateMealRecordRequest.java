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

    public UpdateMealRecordRequest() {
    }

    public MealStatus getMealStatus() { return mealStatus; }
    public void setMealStatus(MealStatus mealStatus) { this.mealStatus = mealStatus; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
