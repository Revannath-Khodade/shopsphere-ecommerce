package com.shopsphere.exception;

/**
 * Specialization of a "not found" error specifically for Order lookups,
 * kept distinct from the generic ResourceNotFoundException so order-related
 * flows (cancellation, tracking, history) can be handled/logged distinctly
 * if needed. Mapped to HTTP 404.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(String orderNumber) {
        super(String.format("Order not found: '%s'", orderNumber));
    }
}
