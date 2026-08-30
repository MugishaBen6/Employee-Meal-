package com.emeal.controller;

import com.emeal.dto.response.ApiResponse;
import com.emeal.dto.response.DailyReportSummaryDTO;
import com.emeal.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<DailyReportSummaryDTO>> getDailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department) {
        DailyReportSummaryDTO summary = reportService.generateDailyReportSummary(date, department);
        return ResponseEntity.ok(ApiResponse.success("Daily report data retrieved", summary));
    }

    @GetMapping("/daily/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<byte[]> downloadDailyExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department) throws IOException {
        LocalDate reportDate = (date != null) ? date : LocalDate.now();
        byte[] excelBytes = reportService.generateDailyExcelReport(reportDate, department);

        String filename = "Daily_Meal_Report_" + reportDate + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/daily/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<byte[]> downloadDailyPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department) {
        LocalDate reportDate = (date != null) ? date : LocalDate.now();
        byte[] pdfBytes = reportService.generateDailyPdfReport(reportDate, department);

        String filename = "Daily_Meal_Report_" + reportDate + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
