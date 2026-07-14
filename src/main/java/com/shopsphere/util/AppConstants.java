package com.shopsphere.util;

/**
 * Centralized application-wide constants. Keeping these in one place avoids
 * "magic strings/numbers" scattered across the service layer.
 */
public final class AppConstants {

    private AppConstants() {
        // Utility class - prevent instantiation
    }

    // ------------------------- Pagination -------------------------
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "asc";
    public static final int MAX_PAGE_SIZE = 100;

    // ------------------------- Review -------------------------
    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    // ------------------------- Order -------------------------
    public static final String ORDER_NUMBER_PREFIX = "SS";

    // ------------------------- Misc -------------------------
    public static final String DEFAULT_CURRENCY = "INR";
}
