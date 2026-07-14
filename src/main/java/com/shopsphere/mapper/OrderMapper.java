package com.shopsphere.mapper;

import com.shopsphere.dto.response.OrderItemResponse;
import com.shopsphere.dto.response.OrderResponse;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.OrderItem;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.ProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts between the Order/OrderItem entities and their response DTOs.
 * Delegates address and payment sub-mapping to their dedicated mappers to
 * keep each mapper focused on a single entity graph.
 */
@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final AddressMapper addressMapper;
    private final PaymentMapper paymentMapper;

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getOrderItems() == null
                ? List.of()
                : order.getOrderItems().stream()
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingFee(order.getShippingFee())
                .taxAmount(order.getTaxAmount())
                .notes(order.getNotes())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .shippingAddress(addressMapper.toResponse(order.getShippingAddress()))
                .billingAddress(addressMapper.toResponse(order.getBillingAddress()))
                .items(itemResponses)
                .payment(paymentMapper.toResponse(order.getPayment()))
                .orderDate(order.getOrderDate())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderItemResponse toItemResponse(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }
        Product product = orderItem.getProduct();
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .productImageUrl(product != null ? findPrimaryImageUrl(product) : null)
                .quantity(orderItem.getQuantity())
                .price(orderItem.getPrice())
                .subtotal(orderItem.getSubtotal())
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
