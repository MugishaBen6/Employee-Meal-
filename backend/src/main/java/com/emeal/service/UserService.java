package com.emeal.service;

import com.emeal.dto.request.ApproveUserRequest;
import com.emeal.dto.request.ChangePasswordRequest;
import com.emeal.dto.request.CreateUserRequest;
import com.emeal.dto.request.UpdateUserRequest;
import com.emeal.dto.response.PageResponse;
import com.emeal.dto.response.UserDTO;
import com.emeal.entity.Role;
import com.emeal.entity.User;
import com.emeal.entity.UserStatus;
import com.emeal.exception.DuplicateResourceException;
import com.emeal.exception.ResourceNotFoundException;
import com.emeal.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDTO> searchUsers(String query, Role role, UserStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result = userRepository.searchUsers(query, role, status, pageable);
        List<UserDTO> dtos = result.getContent().stream().map(UserDTO::fromEntity).toList();
        return PageResponse.fromPage(result, dtos);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserDTO.fromEntity(user);
    }

    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);

        auditLogService.logAction("CREATE_USER", "USER", saved.getId().toString(),
                "Created new user " + saved.getUsername() + " with role " + saved.getRole());

        return UserDTO.fromEntity(saved);
    }

    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!user.getEmail().equalsIgnoreCase(request.getEmail().trim()) && userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already in use by another user");
        }

        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());

        User updated = userRepository.save(user);

        auditLogService.logAction("UPDATE_USER", "USER", updated.getId().toString(),
                "Updated user " + updated.getUsername());

        return UserDTO.fromEntity(updated);
    }

    @Transactional
    public UserDTO approveUser(Long id, ApproveUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setStatus(UserStatus.ACTIVE);
        if (request != null && request.getRole() != null) {
            user.setRole(request.getRole());
        }

        User saved = userRepository.save(user);

        auditLogService.logAction("APPROVE_USER", "USER", saved.getId().toString(),
                "Approved user " + saved.getUsername() + " and assigned role " + saved.getRole());

        return UserDTO.fromEntity(saved);
    }

    @Transactional
    public void rejectUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);

        auditLogService.logAction("REJECT_USER", "USER", user.getId().toString(),
                "Rejected registration request for user: " + user.getUsername());
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.logAction("RESET_PASSWORD", "USER", user.getId().toString(),
                "Reset password for user " + user.getUsername());
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        UserStatus newStatus = (user.getStatus() == UserStatus.ACTIVE) ? UserStatus.INACTIVE : UserStatus.ACTIVE;
        user.setStatus(newStatus);
        userRepository.save(user);

        auditLogService.logAction("TOGGLE_USER_STATUS", "USER", user.getId().toString(),
                "Changed user " + user.getUsername() + " status to " + newStatus);
    }
}
