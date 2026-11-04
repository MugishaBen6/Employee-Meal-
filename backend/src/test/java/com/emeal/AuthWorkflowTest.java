package com.emeal;

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
import com.emeal.repository.PasswordResetTokenRepository;
import com.emeal.repository.UserRepository;
import com.emeal.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class AuthWorkflowTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testInitialAdminSetup_Success_AndSubsequentAdminsRequireApproval() {
        assertTrue(authService.isInitialAdminSetupNeeded());

        SetupAdminRequest request = new SetupAdminRequest();
        request.setFirstName("Master");
        request.setLastName("Admin");
        request.setUsername("masteradmin");
        request.setEmail("admin@company.com");
        request.setPassword("Secret123!");

        UserDTO created = authService.setupInitialAdmin(request);
        assertNotNull(created);
        assertEquals("masteradmin", created.getUsername());
        assertEquals(Role.ADMIN, created.getRole());
        assertEquals(UserStatus.ACTIVE, created.getStatus());

        assertFalse(authService.isInitialAdminSetupNeeded());

        // Security rule since commit 11d73cd: once an admin exists, /auth/setup-admin no longer
        // grants an active admin. It becomes "Create Admin Account" (same as /auth/register with
        // role ADMIN): the account is PENDING_APPROVAL and cannot log in until an existing admin
        // approves it. Both DuplicateResourceException and BadRequestException map to HTTP 400.

        // Repeating the same request cannot create or overwrite an account.
        assertThrows(DuplicateResourceException.class, () -> authService.setupInitialAdmin(request));

        // A different person using the setup page after initial setup gets no active admin access.
        SetupAdminRequest second = new SetupAdminRequest();
        second.setFirstName("Second");
        second.setLastName("Admin");
        second.setUsername("secondadmin");
        second.setEmail("second@company.com");
        second.setPassword("Secret456!");

        UserDTO pending = authService.setupInitialAdmin(second);
        assertEquals(Role.ADMIN, pending.getRole());
        assertEquals(UserStatus.PENDING_APPROVAL, pending.getStatus());

        BadRequestException loginBlocked = assertThrows(BadRequestException.class,
                () -> authService.login(new LoginRequest("secondadmin", "Secret456!")));
        assertTrue(loginBlocked.getMessage().contains("pending administrator approval"));

        // Still exactly one ACTIVE admin: the original master admin.
        long activeAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.getStatus() == UserStatus.ACTIVE)
                .count();
        assertEquals(1, activeAdmins);
    }

    @Test
    void testPublicRegistration_CreatedAsPendingApproval_CannotLoginUntilApproved() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Employee");
        request.setUsername("johnemp");
        request.setEmail("john@company.com");
        request.setPassword("Pass1234!");
        request.setConfirmPassword("Pass1234!");

        UserDTO registered = authService.register(request);
        assertNotNull(registered);
        assertEquals(UserStatus.PENDING_APPROVAL, registered.getStatus());

        // Attempt login while pending approval must throw BadRequestException
        LoginRequest loginRequest = new LoginRequest("johnemp", "Pass1234!");
        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(loginRequest));
        assertTrue(ex.getMessage().contains("pending administrator approval"));

        // Once approved, login must succeed
        User user = userRepository.findById(registered.getId()).orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        JwtResponse jwt = authService.login(loginRequest);
        assertNotNull(jwt.getToken());
        assertEquals("johnemp", jwt.getUser().getUsername());
    }

    @Test
    void testForgotPassword_AndResetPasswordFlow() {
        // Setup active user
        SetupAdminRequest adminReq = new SetupAdminRequest();
        adminReq.setFirstName("Admin");
        adminReq.setLastName("User");
        adminReq.setUsername("adminuser");
        adminReq.setEmail("resetme@company.com");
        adminReq.setPassword("OldPassword123!");
        authService.setupInitialAdmin(adminReq);

        // Request forgot password
        authService.forgotPassword(new ForgotPasswordRequest("resetme@company.com"));

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll();
        assertEquals(1, tokens.size());
        PasswordResetToken tokenEntity = tokens.get(0);
        assertNull(tokenEntity.getUsedAt());
        assertFalse(tokenEntity.isExpired());
    }
}
