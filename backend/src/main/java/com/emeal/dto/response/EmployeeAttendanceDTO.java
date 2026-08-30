package com.emeal.dto.response;

import com.emeal.entity.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeAttendanceDTO {

    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String fullName;
    private String telephone;
    private String email;
    private String position;
    private String department;
    private EmployeeStatus status;
    private LocalDate mealDate;
    private String mealStatus; // "ATE", "DID_NOT_EAT", "NOT_RECORDED"
    private BigDecimal amount;
    private String currency;
    private Long mealRecordId;
    private String recordedBy;
    private LocalDateTime mealRecordedAt;

    public EmployeeAttendanceDTO() {
    }

    public EmployeeAttendanceDTO(Long id, String employeeCode, String firstName, String lastName, String fullName,
                                 String telephone, String email, String position, String department,
                                 EmployeeStatus status, LocalDate mealDate, String mealStatus,
                                 BigDecimal amount, String currency, Long mealRecordId, String recordedBy,
                                 LocalDateTime mealRecordedAt) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.telephone = telephone;
        this.email = email;
        this.position = position;
        this.department = department;
        this.status = status;
        this.mealDate = mealDate;
        this.mealStatus = mealStatus;
        this.amount = amount;
        this.currency = currency;
        this.mealRecordId = mealRecordId;
        this.recordedBy = recordedBy;
        this.mealRecordedAt = mealRecordedAt;
    }

    public static EmployeeAttendanceDTOBuilder builder() {
        return new EmployeeAttendanceDTOBuilder();
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

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) { this.status = status; }

    public LocalDate getMealDate() { return mealDate; }
    public void setMealDate(LocalDate mealDate) { this.mealDate = mealDate; }

    public String getMealStatus() { return mealStatus; }
    public void setMealStatus(String mealStatus) { this.mealStatus = mealStatus; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Long getMealRecordId() { return mealRecordId; }
    public void setMealRecordId(Long mealRecordId) { this.mealRecordId = mealRecordId; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public LocalDateTime getMealRecordedAt() { return mealRecordedAt; }
    public void setMealRecordedAt(LocalDateTime mealRecordedAt) { this.mealRecordedAt = mealRecordedAt; }

    public static class EmployeeAttendanceDTOBuilder {
        private Long id;
        private String employeeCode;
        private String firstName;
        private String lastName;
        private String fullName;
        private String telephone;
        private String email;
        private String position;
        private String department;
        private EmployeeStatus status;
        private LocalDate mealDate;
        private String mealStatus;
        private BigDecimal amount;
        private String currency;
        private Long mealRecordId;
        private String recordedBy;
        private LocalDateTime mealRecordedAt;

        EmployeeAttendanceDTOBuilder() {}

        public EmployeeAttendanceDTOBuilder id(Long id) { this.id = id; return this; }
        public EmployeeAttendanceDTOBuilder employeeCode(String employeeCode) { this.employeeCode = employeeCode; return this; }
        public EmployeeAttendanceDTOBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public EmployeeAttendanceDTOBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public EmployeeAttendanceDTOBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public EmployeeAttendanceDTOBuilder telephone(String telephone) { this.telephone = telephone; return this; }
        public EmployeeAttendanceDTOBuilder email(String email) { this.email = email; return this; }
        public EmployeeAttendanceDTOBuilder position(String position) { this.position = position; return this; }
        public EmployeeAttendanceDTOBuilder department(String department) { this.department = department; return this; }
        public EmployeeAttendanceDTOBuilder status(EmployeeStatus status) { this.status = status; return this; }
        public EmployeeAttendanceDTOBuilder mealDate(LocalDate mealDate) { this.mealDate = mealDate; return this; }
        public EmployeeAttendanceDTOBuilder mealStatus(String mealStatus) { this.mealStatus = mealStatus; return this; }
        public EmployeeAttendanceDTOBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public EmployeeAttendanceDTOBuilder currency(String currency) { this.currency = currency; return this; }
        public EmployeeAttendanceDTOBuilder mealRecordId(Long mealRecordId) { this.mealRecordId = mealRecordId; return this; }
        public EmployeeAttendanceDTOBuilder recordedBy(String recordedBy) { this.recordedBy = recordedBy; return this; }
        public EmployeeAttendanceDTOBuilder mealRecordedAt(LocalDateTime mealRecordedAt) { this.mealRecordedAt = mealRecordedAt; return this; }

        public EmployeeAttendanceDTO build() {
            return new EmployeeAttendanceDTO(id, employeeCode, firstName, lastName, fullName, telephone, email,
                    position, department, status, mealDate, mealStatus, amount, currency, mealRecordId, recordedBy, mealRecordedAt);
        }
    }
}
