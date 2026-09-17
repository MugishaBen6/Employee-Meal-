package com.emeal.controller;

import com.emeal.dto.excel.ExcelEmployeeRowDTO;
import com.emeal.dto.excel.ExcelImportConfirmRequest;
import com.emeal.dto.excel.ExcelImportPreviewResponse;
import com.emeal.dto.excel.ExcelImportResultResponse;
import com.emeal.dto.request.CreateEmployeeRequest;
import com.emeal.dto.request.UpdateEmployeeRequest;
import com.emeal.dto.response.ApiResponse;
import com.emeal.dto.response.EmployeeAttendancePageResponse;
import com.emeal.dto.response.EmployeeDTO;
import com.emeal.dto.response.PageResponse;
import com.emeal.entity.EmployeeStatus;
import com.emeal.service.EmployeeService;
import com.emeal.service.ExcelImportService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ExcelImportService excelImportService;

    public EmployeeController(EmployeeService employeeService, ExcelImportService excelImportService) {
        this.employeeService = employeeService;
        this.excelImportService = excelImportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeDTO>>> searchEmployees(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<EmployeeDTO> result = employeeService.searchEmployees(query, department, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully", result));
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<EmployeeAttendancePageResponse>> getEmployeeAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String mealStatus,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        EmployeeAttendancePageResponse response = employeeService.getEmployeeAttendancePage(
                date, query, department, mealStatus, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Employee attendance retrieved successfully", response));
    }

    @GetMapping("/quick-search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<List<EmployeeDTO>>> quickSearch(@RequestParam String query) {
        List<EmployeeDTO> result = employeeService.quickSearch(query);
        return ResponseEntity.ok(ApiResponse.success("Quick search results", result));
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<List<String>>> getAllDepartments() {
        return ResponseEntity.ok(ApiResponse.success("Departments retrieved", employeeService.getAllDepartments()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Employee retrieved", employeeService.getEmployeeById(id)));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getEmployeeByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success("Employee retrieved", employeeService.getEmployeeByCode(code)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<EmployeeDTO>> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeDTO created = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Employee created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<EmployeeDTO>> updateEmployee(@PathVariable Long id,
                                                                    @Valid @RequestBody UpdateEmployeeRequest request) {
        EmployeeDTO updated = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<Void>> deactivateEmployee(@PathVariable Long id) {
        employeeService.deactivateEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated successfully"));
    }

    @PostMapping("/bulk-delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<Void>> bulkDeleteEmployees(@RequestBody List<Long> ids) {
        employeeService.bulkDeleteEmployees(ids);
        return ResponseEntity.ok(ApiResponse.success("Selected employees deleted successfully"));
    }

    // ==========================================
    // EXCEL IMPORT ENDPOINTS
    // ==========================================

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] excelBytes = excelImportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employee_import_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGING_DIRECTOR', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<ExcelImportPreviewResponse>> previewExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mealDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mealDate) {
        ExcelImportPreviewResponse preview = excelImportService.previewExcel(file, mealDate);
        return ResponseEntity.ok(ApiResponse.success("Excel parsed and validated successfully", preview));
    }

    @PostMapping("/import/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGING_DIRECTOR', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<ExcelImportResultResponse>> confirmImport(
            @RequestBody ExcelImportConfirmRequest request) {
        ExcelImportResultResponse result = excelImportService.confirmImport(request);
        return ResponseEntity.ok(ApiResponse.success("Import completed successfully", result));
    }

    @PostMapping("/import/error-report")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGING_DIRECTOR', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> downloadErrorReport(@RequestBody List<ExcelEmployeeRowDTO> failedRows) {
        byte[] excelBytes = excelImportService.generateErrorReport(failedRows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employee_import_errors.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
