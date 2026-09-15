package com.emeal.controller;

import com.emeal.dto.excel.ExcelEmployeeRowDTO;
import com.emeal.dto.excel.ExcelImportConfirmRequest;
import com.emeal.dto.excel.ExcelImportPreviewResponse;
import com.emeal.dto.excel.ExcelImportResultResponse;
import com.emeal.dto.response.ApiResponse;
import com.emeal.service.ExcelImportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/employees/import")
public class ExcelImportController {

    private final ExcelImportService excelImportService;

    public ExcelImportController(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGING_DIRECTOR', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] excelBytes = excelImportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employee_import_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<ExcelImportPreviewResponse>> previewExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mealDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mealDate) {
        ExcelImportPreviewResponse preview = excelImportService.previewExcel(file, mealDate);
        return ResponseEntity.ok(ApiResponse.success("Excel parsed and validated successfully", preview));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<ExcelImportResultResponse>> confirmImport(
            @RequestBody ExcelImportConfirmRequest request) {
        ExcelImportResultResponse result = excelImportService.confirmImport(request);
        return ResponseEntity.ok(ApiResponse.success("Import completed successfully", result));
    }

    @PostMapping("/error-report")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<byte[]> downloadErrorReport(@RequestBody List<ExcelEmployeeRowDTO> failedRows) {
        byte[] excelBytes = excelImportService.generateErrorReport(failedRows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employee_import_errors.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
