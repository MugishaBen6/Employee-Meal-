package com.emeal.dto.response;

import com.emeal.entity.Role;
import com.emeal.entity.User;
import com.emeal.entity.UserStatus;

import java.time.LocalDateTime;

public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;

    public UserDTO() {
    }

    public UserDTO(Long id, String username, String email, String firstName, String lastName, String fullName, Role role, UserStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static UserDTOBuilder builder() {
        return new UserDTOBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static class UserDTOBuilder {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String fullName;
        private Role role;
        private UserStatus status;
        private LocalDateTime createdAt;

        UserDTOBuilder() {}

        public UserDTOBuilder id(Long id) { this.id = id; return this; }
        public UserDTOBuilder username(String username) { this.username = username; return this; }
        public UserDTOBuilder email(String email) { this.email = email; return this; }
        public UserDTOBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public UserDTOBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public UserDTOBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public UserDTOBuilder role(Role role) { this.role = role; return this; }
        public UserDTOBuilder status(UserStatus status) { this.status = status; return this; }
        public UserDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public UserDTO build() {
            return new UserDTO(id, username, email, firstName, lastName, fullName, role, status, createdAt);
        }
    }
}
