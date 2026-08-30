package com.emeal.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "settings")
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 50)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 255)
    private String settingValue;

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Settings() {
    }

    public Settings(Long id, String settingKey, String settingValue, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SettingsBuilder builder() {
        return new SettingsBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class SettingsBuilder {
        private Long id;
        private String settingKey;
        private String settingValue;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        SettingsBuilder() {}

        public SettingsBuilder id(Long id) { this.id = id; return this; }
        public SettingsBuilder settingKey(String settingKey) { this.settingKey = settingKey; return this; }
        public SettingsBuilder settingValue(String settingValue) { this.settingValue = settingValue; return this; }
        public SettingsBuilder description(String description) { this.description = description; return this; }
        public SettingsBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public SettingsBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Settings build() {
            return new Settings(id, settingKey, settingValue, description, createdAt, updatedAt);
        }
    }
}
