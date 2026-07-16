package com.shopsphere.service;

import com.shopsphere.dto.request.OrderRequest;
import com.shopsphere.dto.response.OrderResponse;
import com.shopsphere.dto.response.PaginationResponse;
import com.shopsphere.entity.enums.OrderStatus;

public interface OrderService {

    /** Places an order from the customer's current cart contents, then clears the cart. */
    OrderResponse placeOrder(Long userId, OrderRequest request);

    OrderResponse cancelOrder(Long userId, Long orderId);

    PaginationResponse<OrderResponse> getOrderHistory(Long userId, int page, int size);

    OrderResponse getOrderDetails(Long userId, Long orderId);

    OrderResponse getOrderByOrderNumber(String orderNumber);

    /**
     * Admin-only: lists every order in the system, optionally filtered by
     * status. No ownership check - callers must be ROLE_ADMIN.
     */
    PaginationResponse<OrderResponse> getAllOrders(OrderStatus status, int page, int size);
}
