package com.shopsphere.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Reusable, framework-independent validation helpers for business rules that
 * go beyond simple Bean Validation annotations (e.g. cross-field checks
 * performed inside service methods).
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9\\-\\s]{7,20}$");

    private ValidationUtil() {
        // Utility class - prevent instantiation
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNonNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0;
    }

    public static boolean isValidQuantity(Integer quantity) {
        return quantity != null && quantity > 0;
    }

    public static boolean isValidRating(Integer rating) {
        return rating != null && rating >= AppConstants.MIN_RATING && rating <= AppConstants.MAX_RATING;
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
