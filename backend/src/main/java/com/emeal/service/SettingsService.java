package com.emeal.service;

import com.emeal.dto.response.SettingsDTO;
import com.emeal.entity.Settings;
import com.emeal.exception.BadRequestException;
import com.emeal.exception.MisconfigurationException;
import com.emeal.exception.ResourceNotFoundException;
import com.emeal.repository.SettingsRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);
    public static final String STANDARD_MEAL_PRICE_KEY = "STANDARD_MEAL_PRICE";

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

    /**
     * The configured standard meal price (production: 600.00 RWF, set by migration V5).
     *
     * <p>Never silently substitutes a default: if the setting is missing, not a number,
     * or not positive, the error is logged and a {@link MisconfigurationException} is
     * thrown so no meal is charged with a guessed price.
     */
    @Transactional(readOnly = true)
    public BigDecimal getStandardMealPrice() {
        String val = getSettingValue(STANDARD_MEAL_PRICE_KEY, null);
        if (val == null || val.isBlank()) {
            log.error("Setting {} is missing; meal prices cannot be determined", STANDARD_MEAL_PRICE_KEY);
            throw new MisconfigurationException(
                    "The standard meal price is not configured. An administrator must set it in Settings.");
        }
        BigDecimal price;
        try {
            price = new BigDecimal(val.trim());
        } catch (NumberFormatException e) {
            log.error("Setting {} has a non-numeric value '{}'", STANDARD_MEAL_PRICE_KEY, val);
            throw new MisconfigurationException(
                    "The standard meal price setting is invalid. An administrator must correct it in Settings.");
        }
        if (price.signum() <= 0) {
            log.error("Setting {} must be positive but is '{}'", STANDARD_MEAL_PRICE_KEY, val);
            throw new MisconfigurationException(
                    "The standard meal price setting is invalid. An administrator must correct it in Settings.");
        }
        return price;
    }

    @Transactional
    public SettingsDTO updateSetting(String key, String newValue) {
        Settings setting = settingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + key));
        if (STANDARD_MEAL_PRICE_KEY.equals(key)) {
            newValue = validateMealPrice(newValue);
        }

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

    /** Rejects blank, non-numeric, zero and negative prices. Returns the trimmed value. */
    private String validateMealPrice(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Standard meal price is required.");
        }
        String trimmed = value.trim();
        BigDecimal price;
        try {
            price = new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Standard meal price must be a number.");
        }
        if (price.signum() <= 0) {
            throw new BadRequestException("Standard meal price must be greater than 0.");
        }
        return trimmed;
    }
}

