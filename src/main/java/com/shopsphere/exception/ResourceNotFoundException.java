package com.shopsphere.exception;

/**
 * Thrown when a requested resource (entity) cannot be found by its identifier
 * or lookup key. Mapped to HTTP 404 by the GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience constructor for the common "Entity not found by field" case.
     * Example: new ResourceNotFoundException("Product", "id", 42L)
     *          -> "Product not found with id: '42'"
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
