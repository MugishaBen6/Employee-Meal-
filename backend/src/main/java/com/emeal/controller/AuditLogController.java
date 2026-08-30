package com.emeal.controller;

import com.emeal.dto.response.ApiResponse;
import com.emeal.dto.response.AuditLogDTO;
import com.emeal.dto.response.PageResponse;
import com.emeal.service.AuditLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> searchAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        PageResponse<AuditLogDTO> result = auditLogService.searchAuditLogs(username, userRole, action, startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved", result));
    }
}
