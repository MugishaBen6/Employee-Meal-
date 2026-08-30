package com.emeal.dto.response;

import com.emeal.entity.Settings;

public class SettingsDTO {

    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;

    public SettingsDTO() {
    }

    public SettingsDTO(Long id, String settingKey, String settingValue, String description) {
        this.id = id;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
    }

    public static SettingsDTOBuilder builder() {
        return new SettingsDTOBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public static SettingsDTO fromEntity(Settings s) {
        return SettingsDTO.builder()
                .id(s.getId())
                .settingKey(s.getSettingKey())
                .settingValue(s.getSettingValue())
                .description(s.getDescription())
                .build();
    }

    public static class SettingsDTOBuilder {
        private Long id;
        private String settingKey;
        private String settingValue;
        private String description;

        SettingsDTOBuilder() {}

        public SettingsDTOBuilder id(Long id) { this.id = id; return this; }
        public SettingsDTOBuilder settingKey(String settingKey) { this.settingKey = settingKey; return this; }
        public SettingsDTOBuilder settingValue(String settingValue) { this.settingValue = settingValue; return this; }
        public SettingsDTOBuilder description(String description) { this.description = description; return this; }

        public SettingsDTO build() {
            return new SettingsDTO(id, settingKey, settingValue, description);
        }
    }
}
