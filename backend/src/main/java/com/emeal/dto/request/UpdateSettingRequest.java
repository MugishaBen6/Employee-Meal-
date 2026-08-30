package com.emeal.dto.request;

import jakarta.validation.constraints.NotBlank;

public class UpdateSettingRequest {

    @NotBlank(message = "Setting value is required")
    private String settingValue;

    public UpdateSettingRequest() {
    }

    public UpdateSettingRequest(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
}
