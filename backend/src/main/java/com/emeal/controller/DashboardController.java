package com.emeal.controller;

import com.emeal.dto.response.ApiResponse;
import com.emeal.dto.response.DashboardStatsResponse;
import com.emeal.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStatistics() {
        DashboardStatsResponse stats = dashboardService.getDashboardStatistics();
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved", stats));
    }
}
