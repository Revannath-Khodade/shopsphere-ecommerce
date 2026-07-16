package com.shopsphere.controller;

import com.shopsphere.dto.ApiResponse;
import com.shopsphere.dto.response.WishlistResponse;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Saved-for-later product wishlist for the authenticated user")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{productId}")
    @Operation(summary = "Add a product to the current user's wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Product id") @PathVariable Long productId) {
        log.info("Adding productId={} to wishlist of userId={}", productId, currentUser.getId());
        WishlistResponse response = wishlistService.addToWishlist(currentUser.getId(), productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product added to wishlist", response));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove a product from the current user's wishlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Product id") @PathVariable Long productId) {
        log.info("Removing productId={} from wishlist of userId={}", productId, currentUser.getId());
        wishlistService.removeFromWishlist(currentUser.getId(), productId);
        return ResponseEntity.ok(ApiResponse.message("Product removed from wishlist"));
    }

    @GetMapping
    @Operation(summary = "View the current user's wishlist")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getWishlist(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<WishlistResponse> response = wishlistService.getWishlist(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
