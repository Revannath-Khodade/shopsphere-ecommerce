package com.shopsphere.mapper;

import com.shopsphere.dto.response.ProductImageResponse;
import com.shopsphere.dto.response.ProductResponse;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.ProductImage;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts between the Product entity and ProductResponse.
 * <p>
 * Note: average rating / review count are deliberately NOT computed here -
 * that requires a ReviewRepository query, which is a service-layer concern.
 * ProductServiceImpl enriches the mapped response with those two fields
 * after calling {@link #toResponse(Product)}, keeping this mapper a pure,
 * side-effect-free structural converter.
 */
@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        List<ProductImageResponse> imageResponses = product.getImages() == null
                ? List.of()
                : product.getImages().stream()
                    .sorted(Comparator.comparing(ProductImage::getDisplayOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(this::toImageResponse)
                    .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .stockQuantity(product.getStockQuantity())
                .brand(product.getBrand())
                .active(product.getActive())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .sellerId(product.getSeller() != null ? product.getSeller().getId() : null)
                .sellerName(product.getSeller() != null
                        ? product.getSeller().getFirstName() + " " + product.getSeller().getLastName()
                        : null)
                .images(imageResponses)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductImageResponse toImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .altText(image.getAltText())
                .isPrimary(image.getIsPrimary())
                .displayOrder(image.getDisplayOrder())
                .build();
    }
}
