package com.emeal.service;

import com.emeal.config.JwtUtils;
import com.emeal.dto.request.ForgotPasswordRequest;
import com.emeal.dto.request.LoginRequest;
import com.emeal.dto.request.RegisterRequest;
import com.emeal.dto.request.ResetPasswordRequest;
import com.emeal.dto.request.SetupAdminRequest;
import com.emeal.dto.response.JwtResponse;
import com.emeal.dto.response.UserDTO;
import com.emeal.entity.PasswordResetToken;
import com.emeal.entity.Role;
import com.emeal.entity.User;
import com.emeal.entity.UserStatus;
import com.emeal.exception.BadRequestException;
import com.emeal.exception.DuplicateResourceException;
import com.emeal.exception.ResourceNotFoundException;
import com.emeal.repository.PasswordResetTokenRepository;
import com.emeal.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       EmailService emailService,
                       AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public boolean isInitialAdminSetupNeeded() {
        return userRepository.countByRole(Role.ADMIN) == 0;
    }

    @Transactional
    public UserDTO setupInitialAdmin(SetupAdminRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        boolean isFirstAdmin = isInitialAdminSetupNeeded();
        UserStatus status = isFirstAdmin ? UserStatus.ACTIVE : UserStatus.PENDING_APPROVAL;

        User admin = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(Role.ADMIN)
                .status(status)
                .build();

        User saved = userRepository.save(admin);

        auditLogService.logAction("ADMIN_SETUP", "USER", saved.getId().toString(),
                "Administrator account created: " + saved.getUsername() + " (status: " + status + ")");

        return UserDTO.fromEntity(saved);
    }

    @Transactional
    public UserDTO register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and Confirm Password do not match");
        }

        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        Role targetRole = request.getRole() != null ? request.getRole() : Role.HR;
        boolean isFirstAdmin = (targetRole == Role.ADMIN && isInitialAdminSetupNeeded());
        UserStatus status = isFirstAdmin ? UserStatus.ACTIVE : UserStatus.PENDING_APPROVAL;

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(targetRole)
                .status(status)
                .build();

        User saved = userRepository.save(user);

        auditLogService.logAction("USER_REGISTRATION", "USER", saved.getId().toString(),
                "New " + targetRole + " account registered: " + saved.getUsername() + " (" + saved.getEmail() + "), status: " + status);

        return UserDTO.fromEntity(saved);
    }

    public JwtResponse login(LoginRequest loginRequest) {
        String identifier = loginRequest.getUsernameOrEmail().trim();

        User user = userRepository.findByUsername(identifier)
                .orElseGet(() -> userRepository.findByEmail(identifier.toLowerCase())
                        .orElseThrow(() -> new BadRequestException("Invalid username/email or password")));

        if (user.getStatus() == UserStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Your account is pending administrator approval before you can log in.");
        }
        if (user.getStatus() == UserStatus.INACTIVE || user.getStatus() == UserStatus.REJECTED) {
            throw new BadRequestException("Your account has been deactivated or rejected. Please contact an administrator.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        auditLogService.logAction("USER_LOGIN", "USER", user.getId().toString(),
                "User " + user.getUsername() + " (" + user.getRole() + ") logged in successfully");

        return JwtResponse.builder()
                .token(jwt)
                .type("Bearer")
                .user(UserDTO.fromEntity(user))
                .build();
    }

    @Transactional
    public java.util.Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.getEmail().trim();
        User user = userRepository.findByEmail(identifier.toLowerCase())
                .orElseGet(() -> userRepository.findByUsername(identifier).orElse(null));

        if (user == null) {
            throw new BadRequestException("No account found matching '" + identifier + "'");
        }

        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);

        // Remove any existing reset tokens for user
        passwordResetTokenRepository.deleteByUserId(user.getId());

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), rawToken, user.getUsername());

        auditLogService.logAction("FORGOT_PASSWORD_REQUEST", "USER", user.getId().toString(),
                "Password reset token requested for user: " + user.getUsername() + " (" + user.getEmail() + ")");

        return java.util.Map.of(
                "resetToken", rawToken,
                "email", user.getEmail(),
                "username", user.getUsername()
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and Confirm Password do not match");
        }

        String tokenHash = hashToken(request.getToken().trim());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset link"));

        if (resetToken.isExpired()) {
            throw new BadRequestException("This password reset link has expired. Please request a new one.");
        }
        if (resetToken.isUsed()) {
            throw new BadRequestException("This password reset link has already been used.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        auditLogService.logAction("RESET_PASSWORD_COMPLETED", "USER", user.getId().toString(),
                "Password reset completed successfully via token for user: " + user.getUsername());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
