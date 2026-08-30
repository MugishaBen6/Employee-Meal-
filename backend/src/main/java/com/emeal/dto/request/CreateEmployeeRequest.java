package com.emeal.dto.request;

import com.emeal.entity.MealStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateEmployeeRequest {

    @NotBlank(message = "Employee ID is required")
    @Size(max = 50, message = "Employee ID cannot exceed 50 characters")
    private String employeeCode;

    private String employeeName;

    private String firstName;
    private String lastName;

    private String department;

    @NotBlank(message = "Position is required")
    private String position;

    @NotBlank(message = "Telephone number is required")
    private String phone;

    private String email;

    private MealStatus mealStatus; // ATE, DID_NOT_EAT

    private BigDecimal amount; // e.g. 1500.00 or 0.00

    private LocalDate mealDate;

    public CreateEmployeeRequest() {
    }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public MealStatus getMealStatus() { return mealStatus; }
    public void setMealStatus(MealStatus mealStatus) { this.mealStatus = mealStatus; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getMealDate() { return mealDate; }
    public void setMealDate(LocalDate mealDate) { this.mealDate = mealDate; }
}
