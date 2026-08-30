package com.emeal.dto.response;

public class EmployeeAttendancePageResponse {

    private EmployeeAttendanceSummaryDTO summary;
    private PageResponse<EmployeeAttendanceDTO> employees;

    public EmployeeAttendancePageResponse() {
    }

    public EmployeeAttendancePageResponse(EmployeeAttendanceSummaryDTO summary, PageResponse<EmployeeAttendanceDTO> employees) {
        this.summary = summary;
        this.employees = employees;
    }

    public static EmployeeAttendancePageResponseBuilder builder() {
        return new EmployeeAttendancePageResponseBuilder();
    }

    public EmployeeAttendanceSummaryDTO getSummary() { return summary; }
    public void setSummary(EmployeeAttendanceSummaryDTO summary) { this.summary = summary; }

    public PageResponse<EmployeeAttendanceDTO> getEmployees() { return employees; }
    public void setEmployees(PageResponse<EmployeeAttendanceDTO> employees) { this.employees = employees; }

    public static class EmployeeAttendancePageResponseBuilder {
        private EmployeeAttendanceSummaryDTO summary;
        private PageResponse<EmployeeAttendanceDTO> employees;

        EmployeeAttendancePageResponseBuilder() {}

        public EmployeeAttendancePageResponseBuilder summary(EmployeeAttendanceSummaryDTO summary) {
            this.summary = summary;
            return this;
        }

        public EmployeeAttendancePageResponseBuilder employees(PageResponse<EmployeeAttendanceDTO> employees) {
            this.employees = employees;
            return this;
        }

        public EmployeeAttendancePageResponse build() {
            return new EmployeeAttendancePageResponse(summary, employees);
        }
    }
}
