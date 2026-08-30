package com.emeal.service;

import com.emeal.dto.response.AuditLogDTO;
import com.emeal.dto.response.PageResponse;
import com.emeal.entity.AuditLog;
import com.emeal.repository.AuditLogRepository;
import com.emeal.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAction(String action, String entityType, String entityId, String description) {
        String username = "SYSTEM";
        String userRole = "SYSTEM";
        Long userId = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            username = principal.getUsername();
            userId = principal.getId();
            userRole = principal.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst().orElse("UNKNOWN");
        }

        AuditLog log = AuditLog.builder()
                .userId(userId)
                .username(username)
                .userRole(userRole)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress("127.0.0.1")
                .build();

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> searchAuditLogs(String username,
                                                     String userRole,
                                                     String action,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     int page,
                                                     int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        Page<AuditLog> result = auditLogRepository.searchAuditLogs(
                username, userRole, action, startDateTime, endDateTime, pageable
        );

        List<AuditLogDTO> dtos = result.getContent().stream()
                .map(AuditLogDTO::fromEntity)
                .toList();

        return PageResponse.fromPage(result, dtos);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getRecentActivities(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findAll(pageable).getContent().stream()
                .map(AuditLogDTO::fromEntity)
                .toList();
    }
}
