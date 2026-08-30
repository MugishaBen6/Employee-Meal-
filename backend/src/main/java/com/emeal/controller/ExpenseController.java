package com.emeal.controller;

import com.emeal.dto.response.ApiResponse;
import com.emeal.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExpenseSummary() {
        Map<String, Object> summary = expenseService.getExpenseSummary();
        return ResponseEntity.ok(ApiResponse.success("Expense summary retrieved", summary));
    }
}
