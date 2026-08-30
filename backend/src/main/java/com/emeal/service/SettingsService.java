package com.emeal.service;

import com.emeal.dto.response.SettingsDTO;
import com.emeal.entity.Settings;
import com.emeal.exception.ResourceNotFoundException;
import com.emeal.repository.SettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final AuditLogService auditLogService;

    public SettingsService(SettingsRepository settingsRepository, AuditLogService auditLogService) {
        this.settingsRepository = settingsRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<SettingsDTO> getAllSettings() {
        return settingsRepository.findAll().stream()
                .map(SettingsDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public String getSettingValue(String key, String defaultValue) {
        return settingsRepository.findBySettingKey(key)
                .map(Settings::getSettingValue)
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public BigDecimal getStandardMealPrice() {
        String val = getSettingValue("STANDARD_MEAL_PRICE", "1500.00");
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            return new BigDecimal("1500.00");
        }
    }

    @Transactional
    public SettingsDTO updateSetting(String key, String newValue) {
        Settings setting = settingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + key));

        String oldValue = setting.getSettingValue();
        setting.setSettingValue(newValue);
        Settings saved = settingsRepository.save(setting);

        auditLogService.logAction("UPDATE_SETTING", "SETTINGS", key,
                "Updated setting " + key + " from '" + oldValue + "' to '" + newValue + "'");

        return SettingsDTO.fromEntity(saved);
    }
}
