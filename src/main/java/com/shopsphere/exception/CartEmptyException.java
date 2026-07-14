package com.shopsphere.exception;

/**
 * Thrown when an operation (typically checkout) requires a non-empty cart,
 * but the customer's cart contains no items. Mapped to HTTP 400.
 */
public class CartEmptyException extends RuntimeException {

    public CartEmptyException(String message) {
        super(message);
    }

    public CartEmptyException() {
        super("Your cart is empty. Add items before proceeding.");
    }
}
