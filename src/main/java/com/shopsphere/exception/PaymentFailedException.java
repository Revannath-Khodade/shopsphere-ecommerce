package com.shopsphere.exception;

/**
 * Thrown when a payment transaction cannot be processed or completed
 * successfully. Mapped to HTTP 402 (Payment Required).
 */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message) {
        super(message);
    }
}
