package com.emeal.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "user_role", nullable = false, length = 30)
    private String userRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 50)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public AuditLog() {
    }

    public AuditLog(Long id, Long userId, String username, String userRole, String action, String entityType, String entityId, String description, String ipAddress, LocalDateTime timestamp) {
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

    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
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

    public static class AuditLogBuilder {
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

        AuditLogBuilder() {}

        public AuditLogBuilder id(Long id) { this.id = id; return this; }
        public AuditLogBuilder userId(Long userId) { this.userId = userId; return this; }
        public AuditLogBuilder username(String username) { this.username = username; return this; }
        public AuditLogBuilder userRole(String userRole) { this.userRole = userRole; return this; }
        public AuditLogBuilder action(String action) { this.action = action; return this; }
        public AuditLogBuilder entityType(String entityType) { this.entityType = entityType; return this; }
        public AuditLogBuilder entityId(String entityId) { this.entityId = entityId; return this; }
        public AuditLogBuilder description(String description) { this.description = description; return this; }
        public AuditLogBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public AuditLogBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public AuditLog build() {
            return new AuditLog(id, userId, username, userRole, action, entityType, entityId, description, ipAddress, timestamp);
        }
    }
}
