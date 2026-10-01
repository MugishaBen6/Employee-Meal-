package com.emeal.service;

import com.emeal.dto.response.SettingsDTO;
import com.emeal.entity.Settings;
import com.emeal.exception.ResourceNotFoundException;
import com.emeal.repository.SettingsRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final AuditLogService auditLogService;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private final AtomicReference<List<SettingsDTO>> allSettingsCache = new AtomicReference<>();

    public SettingsService(SettingsRepository settingsRepository, AuditLogService auditLogService) {
        this.settingsRepository = settingsRepository;
        this.auditLogService = auditLogService;
    }

    @PostConstruct
    public void initCache() {
        try {
            refreshCache();
        } catch (Exception ignored) {
            // In case DB is not yet migrated on startup
        }
    }

    private void refreshCache() {
        List<Settings> all = settingsRepository.findAll();
        cache.clear();
        for (Settings s : all) {
            if (s.getSettingKey() != null && s.getSettingValue() != null) {
                cache.put(s.getSettingKey(), s.getSettingValue());
            }
        }
        allSettingsCache.set(all.stream().map(SettingsDTO::fromEntity).toList());
    }

    @Transactional(readOnly = true)
    public List<SettingsDTO> getAllSettings() {
        List<SettingsDTO> cached = allSettingsCache.get();
        if (cached != null) {
            return cached;
        }
        List<SettingsDTO> list = settingsRepository.findAll().stream()
                .map(SettingsDTO::fromEntity)
                .toList();
        allSettingsCache.set(list);
        return list;
    }

    @Transactional(readOnly = true)
    public String getSettingValue(String key, String defaultValue) {
        String cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        String val = settingsRepository.findBySettingKey(key)
                .map(Settings::getSettingValue)
                .orElse(defaultValue);
        if (val != null) {
            cache.put(key, val);
        }
        return val != null ? val : defaultValue;
    }

    @Transactional(readOnly = true)
    public BigDecimal getStandardMealPrice() {
        String val = getSettingValue("STANDARD_MEAL_PRICE", "600.00");
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            return new BigDecimal("600.00");
        }
    }

    @Transactional
    public SettingsDTO updateSetting(String key, String newValue) {
        Settings setting = settingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + key));

        String oldValue = setting.getSettingValue();
        setting.setSettingValue(newValue);
        Settings saved = settingsRepository.save(setting);

        // Update in-memory cache immediately
        cache.put(key, newValue);
        allSettingsCache.set(null); // Invalidate list cache to refresh on next read

        auditLogService.logAction("UPDATE_SETTING", "SETTINGS", key,
                "Updated setting " + key + " from '" + oldValue + "' to '" + newValue + "'");

        return SettingsDTO.fromEntity(saved);
    }
}

