package com.emeal.dto.request;

import com.emeal.entity.Role;
import jakarta.validation.constraints.NotNull;

public class ApproveUserRequest {

    @NotNull(message = "Role is required for approval")
    private Role role;

    public ApproveUserRequest() {
    }

    public ApproveUserRequest(Role role) {
        this.role = role;
    }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
