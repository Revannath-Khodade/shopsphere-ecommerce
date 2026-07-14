package com.shopsphere.exception;

/**
 * Thrown when authentication fails or credentials are missing/invalid
 * (e.g. bad login, invalid or expired token). Mapped to HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
