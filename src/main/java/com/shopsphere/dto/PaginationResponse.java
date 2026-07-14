package com.shopsphere.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic wrapper for paginated list responses. Deliberately decoupled from
 * Spring Data's Page<T> so we never leak Spring Data types across the API
 * boundary.
 *
 * @param <T> the type of each element in "content"
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    /**
     * Builds a PaginationResponse from a Spring Data Page, mapping its
     * content list to a new (already-converted) list of DTOs.
     */
    public static <T> PaginationResponse<T> from(Page<?> page, List<T> mappedContent) {
        return PaginationResponse.<T>builder()
                .content(mappedContent)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
