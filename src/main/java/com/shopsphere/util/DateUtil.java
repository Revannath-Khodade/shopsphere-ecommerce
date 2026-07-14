package com.shopsphere.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Small set of date/time helpers shared across the service layer, primarily
 * used for generating human-readable references (e.g. order numbers) and
 * formatting timestamps consistently in logs and responses.
 */
public final class DateUtil {

    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private DateUtil() {
        // Utility class - prevent instantiation
    }

    public static String formatForOrderNumber(LocalDateTime dateTime) {
        return dateTime.format(ORDER_NUMBER_DATE_FORMAT);
    }

    public static String formatForDisplay(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DISPLAY_FORMAT);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
