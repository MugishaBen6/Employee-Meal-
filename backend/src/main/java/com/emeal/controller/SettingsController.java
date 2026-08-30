package com.emeal.controller;

import com.emeal.dto.request.UpdateSettingRequest;
import com.emeal.dto.response.ApiResponse;
import com.emeal.dto.response.SettingsDTO;
import com.emeal.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR')")
    public ResponseEntity<ApiResponse<List<SettingsDTO>>> getAllSettings() {
        return ResponseEntity.ok(ApiResponse.success("Settings retrieved", settingsService.getAllSettings()));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SettingsDTO>> updateSetting(@PathVariable String key,
                                                                  @Valid @RequestBody UpdateSettingRequest request) {
        SettingsDTO updated = settingsService.updateSetting(key, request.getSettingValue());
        return ResponseEntity.ok(ApiResponse.success("Setting updated successfully", updated));
    }
}
