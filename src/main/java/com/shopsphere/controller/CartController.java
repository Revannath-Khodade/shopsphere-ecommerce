package com.shopsphere.controller;

import com.shopsphere.dto.ApiResponse;
import com.shopsphere.dto.request.CartRequest;
import com.shopsphere.dto.response.CartResponse;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Shopping cart for the currently authenticated user. Every operation is
 * implicitly scoped to {@code currentUser.getId()} - there is no path
 * variable for a cart or user id, since a customer can only ever see/modify
 * their own cart.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management for the authenticated user")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "View the current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal CustomUserDetails currentUser) {
        CartResponse response = cartService.getCart(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart", description = "If the product is already in the cart, its quantity is increased.")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                               @Valid @RequestBody CartRequest request) {
        log.info("Adding productId={} to cart of userId={}", request.getProductId(), currentUser.getId());
        CartResponse response = cartService.addItem(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", response));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update the quantity of a cart line item")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Product id") @PathVariable Long productId,
            @Parameter(description = "New quantity (must be positive)") @RequestParam @Positive Integer quantity) {
        log.info("Updating productId={} quantity to {} in cart of userId={}", productId, quantity, currentUser.getId());
        CartResponse response = cartService.updateItemQuantity(currentUser.getId(), productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cart item quantity updated", response));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a single item from the cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Product id") @PathVariable Long productId) {
        log.info("Removing productId={} from cart of userId={}", productId, currentUser.getId());
        CartResponse response = cartService.removeItem(currentUser.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", response));
    }

    @DeleteMapping
    @Operation(summary = "Clear every item from the cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Clearing cart for userId={}", currentUser.getId());
        cartService.clearCart(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.message("Cart cleared successfully"));
    }
}
