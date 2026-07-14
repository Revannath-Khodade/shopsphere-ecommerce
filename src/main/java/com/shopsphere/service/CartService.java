package com.shopsphere.service;

import com.shopsphere.dto.request.CartRequest;
import com.shopsphere.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addItem(Long userId, CartRequest request);

    CartResponse updateItemQuantity(Long userId, Long productId, Integer quantity);

    CartResponse removeItem(Long userId, Long productId);

    void clearCart(Long userId);
}
