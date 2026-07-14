package com.shopsphere.exception;

/**
 * Thrown when an operation would violate a uniqueness constraint
 * (e.g. duplicate email, username, SKU, or category name).
 * Mapped to HTTP 409 (Conflict) by the GlobalExceptionHandler.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
