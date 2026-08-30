package com.emeal.dto.response;

import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;

import java.time.LocalDateTime;

public class EmployeeDTO {

    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String fullName;
    private String department;
    private String position;
    private String phone;
    private String email;
    private EmployeeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmployeeDTO() {
    }

    public EmployeeDTO(Long id, String employeeCode, String firstName, String lastName, String fullName, String department, String position, String phone, String email, EmployeeStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.department = department;
        this.position = position;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EmployeeDTOBuilder builder() {
        return new EmployeeDTOBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

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

    public static EmployeeDTO fromEntity(Employee employee) {
        return EmployeeDTO.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .fullName(employee.getFullName())
                .department(employee.getDepartment())
                .position(employee.getPosition())
                .phone(employee.getPhone())
                .email(employee.getEmail())
                .status(employee.getStatus())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    public static class EmployeeDTOBuilder {
        private Long id;
        private String employeeCode;
        private String firstName;
        private String lastName;
        private String fullName;
        private String department;
        private String position;
        private String phone;
        private String email;
        private EmployeeStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        EmployeeDTOBuilder() {}

        public EmployeeDTOBuilder id(Long id) { this.id = id; return this; }
        public EmployeeDTOBuilder employeeCode(String employeeCode) { this.employeeCode = employeeCode; return this; }
        public EmployeeDTOBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public EmployeeDTOBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public EmployeeDTOBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public EmployeeDTOBuilder department(String department) { this.department = department; return this; }
        public EmployeeDTOBuilder position(String position) { this.position = position; return this; }
        public EmployeeDTOBuilder phone(String phone) { this.phone = phone; return this; }
        public EmployeeDTOBuilder email(String email) { this.email = email; return this; }
        public EmployeeDTOBuilder status(EmployeeStatus status) { this.status = status; return this; }
        public EmployeeDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public EmployeeDTOBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public EmployeeDTO build() {
            return new EmployeeDTO(id, employeeCode, firstName, lastName, fullName, department, position, phone, email, status, createdAt, updatedAt);
        }
    }
}
