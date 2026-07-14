package com.shopsphere.entity.enums;

/**
 * Status of a payment transaction associated with an order.
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED,
    CANCELLED
}
