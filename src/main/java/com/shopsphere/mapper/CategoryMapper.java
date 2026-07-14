package com.shopsphere.mapper;

import com.shopsphere.dto.response.CategoryResponse;
import com.shopsphere.entity.Category;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts between the Category entity and CategoryResponse. Handles the
 * self-referencing parent/subcategory hierarchy explicitly to avoid infinite
 * recursion (subcategories are mapped one level deep only).
 */
@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return toResponse(category, true);
    }

    /**
     * @param includeSubCategories whether to eagerly map the immediate
     *                             subcategories list. Set to false when
     *                             mapping a subcategory itself, to keep the
     *                             tree exactly one level deep in responses.
     */
    public CategoryResponse toResponse(Category category, boolean includeSubCategories) {
        if (category == null) {
            return null;
        }

        List<CategoryResponse> subCategoryResponses = Collections.emptyList();
        if (includeSubCategories && category.getSubCategories() != null) {
            subCategoryResponses = category.getSubCategories().stream()
                    .map(sub -> toResponse(sub, false))
                    .collect(Collectors.toList());
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.getActive())
                .parentCategoryId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
                .parentCategoryName(category.getParentCategory() != null ? category.getParentCategory().getName() : null)
                .subCategories(subCategoryResponses)
                .createdAt(category.getCreatedAt())
                .build();
    }
}
