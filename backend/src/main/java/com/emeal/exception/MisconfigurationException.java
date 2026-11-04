package com.emeal.exception;

/**
 * A required system setting is missing or invalid (for example STANDARD_MEAL_PRICE).
 *
 * <p>The message is written by the application for administrators and is safe to
 * return to clients; it must never contain exception internals or SQL.
 */
public class MisconfigurationException extends RuntimeException {
    public MisconfigurationException(String message) {
        super(message);
    }
}
