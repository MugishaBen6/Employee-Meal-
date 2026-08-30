package com.emeal.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PasswordResetToken() {
    }

    public PasswordResetToken(Long id, User user, String tokenHash, LocalDateTime expiresAt, LocalDateTime usedAt, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    public static PasswordResetTokenBuilder builder() {
        return new PasswordResetTokenBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public static class PasswordResetTokenBuilder {
        private Long id;
        private User user;
        private String tokenHash;
        private LocalDateTime expiresAt;
        private LocalDateTime usedAt;
        private LocalDateTime createdAt;

        PasswordResetTokenBuilder() {}

        public PasswordResetTokenBuilder id(Long id) { this.id = id; return this; }
        public PasswordResetTokenBuilder user(User user) { this.user = user; return this; }
        public PasswordResetTokenBuilder tokenHash(String tokenHash) { this.tokenHash = tokenHash; return this; }
        public PasswordResetTokenBuilder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public PasswordResetTokenBuilder usedAt(LocalDateTime usedAt) { this.usedAt = usedAt; return this; }
        public PasswordResetTokenBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public PasswordResetToken build() {
            return new PasswordResetToken(id, user, tokenHash, expiresAt, usedAt, createdAt);
        }
    }
}
