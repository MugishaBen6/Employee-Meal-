package com.emeal.controller;

import com.emeal.dto.response.ApiResponse;
import com.emeal.dto.response.DailyReportSummaryDTO;
import com.emeal.service.ReportService;
import com.emeal.service.ReportService.ReportPeriod;
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

/**
 * Meal reports for an inclusive date period.
 *
 * <p>All three endpoints accept {@code startDate} and {@code endDate} (From / To, ISO yyyy-MM-dd).
 * The legacy {@code date} parameter is still accepted and means a single day. The JSON report,
 * the Excel file and the PDF are built from the same query, so they always contain exactly the
 * same records.
 */
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department) {
        ReportPeriod period = reportService.resolvePeriod(date, startDate, endDate);
        DailyReportSummaryDTO summary = reportService.generateReportSummary(period, department);
        return ResponseEntity.ok(ApiResponse.success("Report data retrieved", summary));
    }

    @GetMapping("/daily/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<byte[]> downloadDailyExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department) throws IOException {
        ReportPeriod period = reportService.resolvePeriod(date, startDate, endDate);
        byte[] excelBytes = reportService.generateExcelReport(period, department);

        String filename = reportService.reportFileBaseName(period) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/daily/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<byte[]> downloadDailyPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department) {
        ReportPeriod period = reportService.resolvePeriod(date, startDate, endDate);
        byte[] pdfBytes = reportService.generatePdfReport(period, department);

        String filename = reportService.reportFileBaseName(period) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
