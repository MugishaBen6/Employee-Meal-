package com.emeal.controller;

import com.emeal.dto.request.RecordMealRequest;
import com.emeal.dto.request.UpdateMealRecordRequest;
import com.emeal.dto.response.ApiResponse;
import com.emeal.dto.response.MealRecordDTO;
import com.emeal.dto.response.PageResponse;
import com.emeal.dto.response.QuickMealCheckResponse;
import com.emeal.entity.MealStatus;
import com.emeal.service.MealRecordService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/meals")
public class MealRecordController {

    private final MealRecordService mealRecordService;

    public MealRecordController(MealRecordService mealRecordService) {
        this.mealRecordService = mealRecordService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<MealRecordDTO>> recordMeal(@Valid @RequestBody RecordMealRequest request) {
        MealRecordDTO dto = mealRecordService.recordMeal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meal recorded successfully for " + dto.getEmployeeName(), dto));
    }

    @GetMapping("/quick-check")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<QuickMealCheckResponse>> quickCheck(
            @RequestParam String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        QuickMealCheckResponse response = mealRecordService.quickCheck(query, date);
        return ResponseEntity.ok(ApiResponse.success("Quick check details", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<PageResponse<MealRecordDTO>>> searchMealRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) MealStatus status,
            @RequestParam(required = false) String recordedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<MealRecordDTO> result = mealRecordService.searchMealRecords(
                startDate, endDate, employeeId, department, status, recordedBy, page, size
        );
        return ResponseEntity.ok(ApiResponse.success("Meal records retrieved", result));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<List<MealRecordDTO>>> getEmployeeMealHistory(@PathVariable Long employeeId) {
        List<MealRecordDTO> list = mealRecordService.getEmployeeMealHistory(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee meal history", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<MealRecordDTO>> getMealRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Meal record retrieved", mealRecordService.getMealRecordById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<MealRecordDTO>> updateMealRecord(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMealRecordRequest request) {
        MealRecordDTO updated = mealRecordService.updateMealRecord(id, request);
        return ResponseEntity.ok(ApiResponse.success("Meal record updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Void>> deleteMealRecord(@PathVariable Long id) {
        mealRecordService.deleteMealRecord(id);
        return ResponseEntity.ok(ApiResponse.success("Meal record deleted successfully"));
    }
}
