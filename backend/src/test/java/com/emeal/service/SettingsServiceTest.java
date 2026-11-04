package com.emeal.service;

import com.emeal.entity.Settings;
import com.emeal.exception.BadRequestException;
import com.emeal.exception.MisconfigurationException;
import com.emeal.repository.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    private static final String KEY = "STANDARD_MEAL_PRICE";

    @Mock private SettingsRepository settingsRepository;
    @Mock private AuditLogService auditLogService;

    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsService = new SettingsService(settingsRepository, auditLogService);
    }

    private void stored(String key, String value) {
        when(settingsRepository.findBySettingKey(key)).thenReturn(Optional.of(
                Settings.builder().id(1L).settingKey(key).settingValue(value).build()));
    }

    // ---------- reading the price: no silent 600 fallback ----------

    @Test
    void productionPrice600_isReturnedAsConfigured() {
        stored(KEY, "600.00");
        assertEquals(0, new BigDecimal("600.00").compareTo(settingsService.getStandardMealPrice()));
    }

    @Test
    void missingPriceSetting_failsClearly_insteadOfDefaultingTo600() {
        when(settingsRepository.findBySettingKey(KEY)).thenReturn(Optional.empty());
        MisconfigurationException ex = assertThrows(MisconfigurationException.class,
                () -> settingsService.getStandardMealPrice());
        assertTrue(ex.getMessage().contains("not configured"));
    }

    @Test
    void invalidStoredPrice_failsClearly() {
        for (String bad : new String[]{"abc", "0", "0.00", "-5", "   "}) {
            SettingsService fresh = new SettingsService(settingsRepository, auditLogService);
            stored(KEY, bad);
            assertThrows(MisconfigurationException.class, fresh::getStandardMealPrice, "stored value '" + bad + "'");
        }
    }

    // ---------- saving the price: reject zero / negative / non-numeric ----------

    @Test
    void savingZeroNegativeBlankOrNonNumericPrice_isRejected_andNothingIsSaved() {
        stored(KEY, "600.00");
        for (String bad : new String[]{"0", "0.00", "-1", "-600", "abc", "", "  ", null}) {
            assertThrows(BadRequestException.class, () -> settingsService.updateSetting(KEY, bad), "value '" + bad + "'");
        }
        verify(settingsRepository, never()).save(any());
        assertEquals(0, new BigDecimal("600.00").compareTo(settingsService.getStandardMealPrice()),
                "price stays 600 after rejected updates");
    }

    @Test
    void savingPositivePrice_isAccepted_andTrimmed() {
        stored(KEY, "600.00");
        when(settingsRepository.save(any(Settings.class))).thenAnswer(inv -> inv.getArgument(0));

        settingsService.updateSetting(KEY, " 650.50 ");

        ArgumentCaptor<Settings> saved = ArgumentCaptor.forClass(Settings.class);
        verify(settingsRepository).save(saved.capture());
        assertEquals("650.50", saved.getValue().getSettingValue());
        assertEquals(0, new BigDecimal("650.50").compareTo(settingsService.getStandardMealPrice()));
    }

    @Test
    void otherSettings_areNotAffectedByPriceValidation() {
        stored("CURRENCY", "RWF");
        when(settingsRepository.save(any(Settings.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("USD", settingsService.updateSetting("CURRENCY", "USD").getSettingValue());
    }
}
