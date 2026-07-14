package com.shopsphere.entity.enums;

/**
 * Lifecycle states of an Order, from creation to completion or cancellation.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED,
    REFUNDED
}
