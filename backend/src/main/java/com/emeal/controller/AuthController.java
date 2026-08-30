package com.emeal.controller;

import com.emeal.dto.request.ForgotPasswordRequest;
import com.emeal.dto.request.LoginRequest;
import com.emeal.dto.request.RegisterRequest;
import com.emeal.dto.request.ResetPasswordRequest;
import com.emeal.dto.request.SetupAdminRequest;
import com.emeal.dto.response.ApiResponse;
import com.emeal.dto.response.JwtResponse;
import com.emeal.dto.response.UserDTO;
import com.emeal.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/setup-status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getSetupStatus() {
        boolean setupNeeded = authService.isInitialAdminSetupNeeded();
        return ResponseEntity.ok(ApiResponse.success("Setup status retrieved", Map.of("setupNeeded", setupNeeded)));
    }

    @PostMapping("/setup-admin")
    public ResponseEntity<ApiResponse<UserDTO>> setupInitialAdmin(@Valid @RequestBody SetupAdminRequest request) {
        UserDTO admin = authService.setupInitialAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Initial master administrator account created successfully. You can now log in.", admin));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserDTO user = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully. Your account is pending administrator approval.", user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("If an account exists with this email, a password reset link has been dispatched."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. You can now log in with your new password."));
    }
}
