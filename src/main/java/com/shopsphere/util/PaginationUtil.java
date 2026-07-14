package com.shopsphere.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds validated, bounded {@link Pageable} instances from raw request
 * parameters. Centralizing this logic prevents each service method from
 * re-implementing the same clamping/sorting rules.
 */
public final class PaginationUtil {

    private PaginationUtil() {
        // Utility class - prevent instantiation
    }

    public static Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? Integer.parseInt(AppConstants.DEFAULT_PAGE_SIZE) : Math.min(size, AppConstants.MAX_PAGE_SIZE);
        String safeSortBy = ValidationUtil.isBlank(sortBy) ? AppConstants.DEFAULT_SORT_BY : sortBy;

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }
}
