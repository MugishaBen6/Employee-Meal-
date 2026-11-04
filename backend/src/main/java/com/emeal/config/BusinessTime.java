package com.emeal.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Business-calendar clock for the company (Africa/Kigali, UTC+2, no DST).
 *
 * <p>Use {@link #today()} wherever a <em>business date</em> is defaulted
 * (meal date, report date, dashboard "today"). It is independent of the JVM
 * default timezone, so the result is correct whether the container runs in
 * UTC (Render's default) or has {@code -Duser.timezone} set.
 *
 * <p>This deliberately does NOT change how entity timestamps
 * ({@code created_at}, {@code updated_at}, audit {@code timestamp}) are
 * written: those are {@code TIMESTAMP WITHOUT TIME ZONE} columns populated by
 * Hibernate/PostgreSQL in the JVM/session zone, and historical rows depend on
 * that staying consistent.
 */
public final class BusinessTime {

    public static final ZoneId ZONE = ZoneId.of("Africa/Kigali");

    private BusinessTime() {
    }

    /** Today's calendar date in Africa/Kigali. */
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** Current wall-clock time in Africa/Kigali, for display text only (e.g. report footers). */
    public static LocalDateTime nowForDisplay() {
        return LocalDateTime.now(ZONE);
    }
}
