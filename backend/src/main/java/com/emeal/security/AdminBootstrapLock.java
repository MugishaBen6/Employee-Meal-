package com.emeal.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Serializes administrator creation so concurrent requests cannot each observe
 * "no administrator exists yet" and all become ACTIVE.
 *
 * <p>Uses a PostgreSQL transaction-level advisory lock ({@code pg_advisory_xact_lock}).
 * The lock is held until the surrounding transaction commits or rolls back, so the
 * next request only checks the admin count after the previous insert is committed.
 * No table, row or schema change is needed. Ordinary reads and writes are not blocked;
 * only callers of {@link #acquire()} wait for each other.
 *
 * <p>On a non-PostgreSQL database (the H2 test profile only) the lock is skipped.
 */
@Component
public class AdminBootstrapLock {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapLock.class);

    /** Fixed application-wide key ("EMEALADM" as ASCII). */
    static final long LOCK_KEY = 0x454D45414C41444DL;

    @PersistenceContext
    private EntityManager entityManager;

    private final boolean postgres;

    public AdminBootstrapLock(DataSource dataSource) {
        boolean isPostgres = false;
        try (Connection connection = dataSource.getConnection()) {
            isPostgres = "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            log.warn("Could not determine database type for admin bootstrap lock", e);
        }
        this.postgres = isPostgres;
        if (!isPostgres) {
            log.warn("Admin bootstrap advisory lock disabled: database is not PostgreSQL (test profile only).");
        }
    }

    /** Must be called inside the transaction that checks for and creates the administrator. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire() {
        if (!postgres) {
            return;
        }
        entityManager.createNativeQuery("SELECT 1 FROM (SELECT pg_advisory_xact_lock(:key)) AS admin_lock")
                .setParameter("key", LOCK_KEY)
                .getSingleResult();
    }

    public boolean isActive() {
        return postgres;
    }
}
