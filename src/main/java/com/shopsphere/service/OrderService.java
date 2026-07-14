package com.shopsphere.service;

import com.shopsphere.dto.request.OrderRequest;
import com.shopsphere.dto.response.OrderResponse;
import com.shopsphere.dto.response.PaginationResponse;

public interface OrderService {

    /** Places an order from the customer's current cart contents, then clears the cart. */
    OrderResponse placeOrder(Long userId, OrderRequest request);

    OrderResponse cancelOrder(Long userId, Long orderId);

    PaginationResponse<OrderResponse> getOrderHistory(Long userId, int page, int size);

    OrderResponse getOrderDetails(Long userId, Long orderId);

    OrderResponse getOrderByOrderNumber(String orderNumber);
}
