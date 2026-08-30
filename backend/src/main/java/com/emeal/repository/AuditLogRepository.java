package com.emeal.repository;

import com.emeal.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
           "(:userRole IS NULL OR a.userRole = :userRole) AND " +
           "(:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%'))) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate)")
    Page<AuditLog> searchAuditLogs(@Param("username") String username,
                                   @Param("userRole") String userRole,
                                   @Param("action") String action,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   Pageable pageable);
}
