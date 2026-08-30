package com.emeal.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 50)
    private String department;

    @Column(nullable = false, length = 50)
    private String position;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Employee() {
    }

    public Employee(Long id, String employeeCode, String firstName, String lastName, String department, String position, String phone, String email, EmployeeStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.position = position;
        this.phone = phone;
        this.email = email;
        this.status = status != null ? status : EmployeeStatus.ACTIVE;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EmployeeBuilder builder() {
        return new EmployeeBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

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

    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public static class EmployeeBuilder {
        private Long id;
        private String employeeCode;
        private String firstName;
        private String lastName;
        private String department;
        private String position;
        private String phone;
        private String email;
        private EmployeeStatus status = EmployeeStatus.ACTIVE;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        EmployeeBuilder() {}

        public EmployeeBuilder id(Long id) { this.id = id; return this; }
        public EmployeeBuilder employeeCode(String employeeCode) { this.employeeCode = employeeCode; return this; }
        public EmployeeBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public EmployeeBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public EmployeeBuilder department(String department) { this.department = department; return this; }
        public EmployeeBuilder position(String position) { this.position = position; return this; }
        public EmployeeBuilder phone(String phone) { this.phone = phone; return this; }
        public EmployeeBuilder email(String email) { this.email = email; return this; }
        public EmployeeBuilder status(EmployeeStatus status) { this.status = status; return this; }
        public EmployeeBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public EmployeeBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Employee build() {
            return new Employee(id, employeeCode, firstName, lastName, department, position, phone, email, status, createdAt, updatedAt);
        }
    }
}
