package com.shopsphere.util;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generates human-readable, externally-facing order references, e.g.
 * "SS-20260710-8F3A21C4". Combines a date segment (for easy sorting/searching)
 * with a short random segment (to guarantee uniqueness without a DB round-trip).
 */
public final class OrderNumberGenerator {

    private OrderNumberGenerator() {
        // Utility class - prevent instantiation
    }

    public static String generate() {
        String datePart = DateUtil.formatForOrderNumber(LocalDateTime.now());
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return AppConstants.ORDER_NUMBER_PREFIX + "-" + datePart + "-" + randomPart;
    }
}
