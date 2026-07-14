package com.shopsphere.service;

import com.shopsphere.dto.response.WishlistResponse;

import java.util.List;

public interface WishlistService {

    WishlistResponse addToWishlist(Long userId, Long productId);

    void removeFromWishlist(Long userId, Long productId);

    List<WishlistResponse> getWishlist(Long userId);
}
