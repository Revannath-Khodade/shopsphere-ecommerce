package com.shopsphere.mapper;

import com.shopsphere.dto.response.CartItemResponse;
import com.shopsphere.dto.response.CartResponse;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.CartItem;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.ProductImage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts between the Cart/CartItem entities and their response DTOs.
 * Also computes derived totals (line-item subtotal, cart total, item count)
 * since those are presentation-layer concerns that should never be persisted.
 */
@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return null;
        }

        List<CartItemResponse> itemResponses = cart.getCartItems() == null
                ? List.of()
                : cart.getCartItems().stream()
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(itemResponses)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    public CartItemResponse toItemResponse(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }
        Product product = cartItem.getProduct();
        BigDecimal subtotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .productImageUrl(product != null ? findPrimaryImageUrl(product) : null)
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(subtotal)
                .availableStock(product != null ? product.getStockQuantity() : null)
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
