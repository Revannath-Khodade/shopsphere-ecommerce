package com.shopsphere.exception;

/**
 * Thrown for generic malformed or semantically invalid requests that don't
 * fit a more specific exception type. Mapped to HTTP 400 (Bad Request).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
