package com.emeal.controller;

import com.emeal.dto.request.ApproveUserRequest;
import com.emeal.dto.request.ChangePasswordRequest;
import com.emeal.dto.request.CreateUserRequest;
import com.emeal.dto.request.UpdateUserRequest;
import com.emeal.dto.response.ApiResponse;
import com.emeal.dto.response.PageResponse;
import com.emeal.dto.response.UserDTO;
import com.emeal.entity.Role;
import com.emeal.entity.UserStatus;
import com.emeal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDTO>>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<UserDTO> result = userService.searchUsers(query, role, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User details retrieved", userService.getUserById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDTO created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateUserRequest request) {
        UserDTO updated = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<UserDTO>> approveUser(@PathVariable Long id,
                                                             @Valid @RequestBody(required = false) ApproveUserRequest request) {
        UserDTO approved = userService.approveUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User approved successfully", approved));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectUser(@PathVariable Long id) {
        userService.rejectUser(id);
        return ResponseEntity.ok(ApiResponse.success("User registration rejected"));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@PathVariable Long id,
                                                             @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("User password reset successfully"));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.success("User status toggled successfully"));
    }
}
