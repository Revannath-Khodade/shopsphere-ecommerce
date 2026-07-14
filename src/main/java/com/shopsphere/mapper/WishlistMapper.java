package com.shopsphere.mapper;

import com.shopsphere.dto.response.WishlistResponse;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.ProductImage;
import com.shopsphere.entity.Wishlist;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class WishlistMapper {

    public WishlistResponse toResponse(Wishlist wishlist) {
        if (wishlist == null) {
            return null;
        }
        Product product = wishlist.getProduct();
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .productImageUrl(product != null ? findPrimaryImageUrl(product) : null)
                .productPrice(product != null ? product.getPrice() : null)
                .productDiscountPrice(product != null ? product.getDiscountPrice() : null)
                .inStock(product != null && product.getStockQuantity() != null && product.getStockQuantity() > 0)
                .createdAt(wishlist.getCreatedAt())
                .build();
    }

    private String findPrimaryImageUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> product.getImages().stream()
                        .min(Comparator.comparing(ProductImage::getDisplayOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(ProductImage::getImageUrl)
                        .orElse(null));
    }
}
