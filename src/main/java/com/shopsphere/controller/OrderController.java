package com.shopsphere.controller;

import com.shopsphere.dto.ApiResponse;
import com.shopsphere.dto.request.OrderRequest;
import com.shopsphere.dto.response.OrderResponse;
import com.shopsphere.dto.response.PaginationResponse;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.service.OrderService;
import com.shopsphere.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Order placement, cancellation, and history for the authenticated customer,
 * plus an admin-only view across every order in the system.
 */
@Slf4j
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement, cancellation, history, and admin order management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place an order from the current cart", description = "Validates stock, decrements inventory, creates a pending payment record, and clears the cart.")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                   @Valid @RequestBody OrderRequest request) {
        log.info("Placing order for userId={}", currentUser.getId());
        OrderResponse response = orderService.placeOrder(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order", description = "Only orders in PENDING, CONFIRMED, or PROCESSING status may be cancelled. Restocks all line items.")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                    @Parameter(description = "Order id") @PathVariable Long id) {
        log.info("Cancelling orderId={} for userId={}", id, currentUser.getId());
        OrderResponse response = orderService.cancelOrder(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get the current user's order history", description = "Paginated, most recent first.")
    public ResponseEntity<ApiResponse<PaginationResponse<OrderResponse>>> getOrderHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        PaginationResponse<OrderResponse> response = orderService.getOrderHistory(currentUser.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by id", description = "Only the order's owner may view it.")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetails(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                        @Parameter(description = "Order id") @PathVariable Long id) {
        OrderResponse response = orderService.getOrderDetails(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/track/{orderNumber}")
    @Operation(summary = "Look up an order by its externally-facing order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByOrderNumber(
            @Parameter(description = "Order number, e.g. SS-20260710-8F3A21C4") @PathVariable String orderNumber) {
        OrderResponse response = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List every order in the system (admin only)", description = "Optionally filtered by status.")
    public ResponseEntity<ApiResponse<PaginationResponse<OrderResponse>>> getAllOrders(
            @Parameter(description = "Filter by order status") @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        PaginationResponse<OrderResponse> response = orderService.getAllOrders(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
