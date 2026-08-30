package com.emeal.dto.response;

import com.emeal.entity.AuditLog;

import java.time.LocalDateTime;

public class AuditLogDTO {

    private Long id;
    private Long userId;
    private String username;
    private String userRole;
    private String action;
    private String entityType;
    private String entityId;
    private String description;
    private String ipAddress;
    private LocalDateTime timestamp;

    public AuditLogDTO() {
    }

    public AuditLogDTO(Long id, Long userId, String username, String userRole, String action, String entityType, String entityId, String description, String ipAddress, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.userRole = userRole;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    public static AuditLogDTOBuilder builder() {
        return new AuditLogDTOBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public static AuditLogDTO fromEntity(AuditLog log) {
        return AuditLogDTO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .userRole(log.getUserRole())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .timestamp(log.getTimestamp())
                .build();
    }

    public static class AuditLogDTOBuilder {
        private Long id;
        private Long userId;
        private String username;
        private String userRole;
        private String action;
        private String entityType;
        private String entityId;
        private String description;
        private String ipAddress;
        private LocalDateTime timestamp;

        AuditLogDTOBuilder() {}

        public AuditLogDTOBuilder id(Long id) { this.id = id; return this; }
        public AuditLogDTOBuilder userId(Long userId) { this.userId = userId; return this; }
        public AuditLogDTOBuilder username(String username) { this.username = username; return this; }
        public AuditLogDTOBuilder userRole(String userRole) { this.userRole = userRole; return this; }
        public AuditLogDTOBuilder action(String action) { this.action = action; return this; }
        public AuditLogDTOBuilder entityType(String entityType) { this.entityType = entityType; return this; }
        public AuditLogDTOBuilder entityId(String entityId) { this.entityId = entityId; return this; }
        public AuditLogDTOBuilder description(String description) { this.description = description; return this; }
        public AuditLogDTOBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public AuditLogDTOBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public AuditLogDTO build() {
            return new AuditLogDTO(id, userId, username, userRole, action, entityType, entityId, description, ipAddress, timestamp);
        }
    }
}
