package com.emeal.dto.response;

import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MealRecordDTO {

    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String department;
    private LocalDate mealDate;
    private MealStatus mealStatus;
    private BigDecimal amount;
    private String recordedBy;
    private LocalDateTime createdAt;

    public MealRecordDTO() {
    }

    public MealRecordDTO(Long id, Long employeeId, String employeeCode, String employeeName, String department, LocalDate mealDate, MealStatus mealStatus, BigDecimal amount, String recordedBy, LocalDateTime createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.department = department;
        this.mealDate = mealDate;
        this.mealStatus = mealStatus;
        this.amount = amount;
        this.recordedBy = recordedBy;
        this.createdAt = createdAt;
    }

    public static MealRecordDTOBuilder builder() {
        return new MealRecordDTOBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDate getMealDate() { return mealDate; }
    public void setMealDate(LocalDate mealDate) { this.mealDate = mealDate; }

    public MealStatus getMealStatus() { return mealStatus; }
    public void setMealStatus(MealStatus mealStatus) { this.mealStatus = mealStatus; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static MealRecordDTO fromEntity(MealRecord record) {
        return MealRecordDTO.builder()
                .id(record.getId())
                .employeeId(record.getEmployee().getId())
                .employeeCode(record.getEmployee().getEmployeeCode())
                .employeeName(record.getEmployee().getFullName())
                .department(record.getEmployee().getDepartment())
                .mealDate(record.getMealDate())
                .mealStatus(record.getMealStatus())
                .amount(record.getAmount())
                .recordedBy(record.getRecordedBy())
                .createdAt(record.getCreatedAt())
                .build();
    }

    public static class MealRecordDTOBuilder {
        private Long id;
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private String department;
        private LocalDate mealDate;
        private MealStatus mealStatus;
        private BigDecimal amount;
        private String recordedBy;
        private LocalDateTime createdAt;

        MealRecordDTOBuilder() {}

        public MealRecordDTOBuilder id(Long id) { this.id = id; return this; }
        public MealRecordDTOBuilder employeeId(Long employeeId) { this.employeeId = employeeId; return this; }
        public MealRecordDTOBuilder employeeCode(String employeeCode) { this.employeeCode = employeeCode; return this; }
        public MealRecordDTOBuilder employeeName(String employeeName) { this.employeeName = employeeName; return this; }
        public MealRecordDTOBuilder department(String department) { this.department = department; return this; }
        public MealRecordDTOBuilder mealDate(LocalDate mealDate) { this.mealDate = mealDate; return this; }
        public MealRecordDTOBuilder mealStatus(MealStatus mealStatus) { this.mealStatus = mealStatus; return this; }
        public MealRecordDTOBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public MealRecordDTOBuilder recordedBy(String recordedBy) { this.recordedBy = recordedBy; return this; }
        public MealRecordDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public MealRecordDTO build() {
            return new MealRecordDTO(id, employeeId, employeeCode, employeeName, department, mealDate, mealStatus, amount, recordedBy, createdAt);
        }
    }
}
