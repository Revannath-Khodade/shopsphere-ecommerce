package com.shopsphere.exception;

/**
 * Thrown when an authenticated user attempts an action they are not
 * permitted to perform (e.g. editing another user's order). Mapped to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
