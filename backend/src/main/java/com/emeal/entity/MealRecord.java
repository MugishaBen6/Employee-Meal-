package com.emeal.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_records", uniqueConstraints = {
    @UniqueConstraint(name = "uk_employee_meal_date", columnNames = {"employee_id", "meal_date"})
})
public class MealRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_status", nullable = false, length = 20)
    private MealStatus mealStatus = MealStatus.ATE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "recorded_by", nullable = false, length = 50)
    private String recordedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MealRecord() {
    }

    public MealRecord(Long id, Employee employee, LocalDate mealDate, MealStatus mealStatus, BigDecimal amount, String recordedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employee = employee;
        this.mealDate = mealDate;
        this.mealStatus = mealStatus != null ? mealStatus : MealStatus.ATE;
        this.amount = amount;
        this.recordedBy = recordedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MealRecordBuilder builder() {
        return new MealRecordBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

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

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class MealRecordBuilder {
        private Long id;
        private Employee employee;
        private LocalDate mealDate;
        private MealStatus mealStatus = MealStatus.ATE;
        private BigDecimal amount;
        private String recordedBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        MealRecordBuilder() {}

        public MealRecordBuilder id(Long id) { this.id = id; return this; }
        public MealRecordBuilder employee(Employee employee) { this.employee = employee; return this; }
        public MealRecordBuilder mealDate(LocalDate mealDate) { this.mealDate = mealDate; return this; }
        public MealRecordBuilder mealStatus(MealStatus mealStatus) { this.mealStatus = mealStatus; return this; }
        public MealRecordBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public MealRecordBuilder recordedBy(String recordedBy) { this.recordedBy = recordedBy; return this; }
        public MealRecordBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public MealRecordBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public MealRecord build() {
            return new MealRecord(id, employee, mealDate, mealStatus, amount, recordedBy, createdAt, updatedAt);
        }
    }
}
