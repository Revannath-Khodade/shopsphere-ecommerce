package com.shopsphere.controller;

import com.shopsphere.dto.ApiResponse;
import com.shopsphere.dto.request.PaymentRequest;
import com.shopsphere.dto.response.PaymentResponse;
import com.shopsphere.entity.enums.PaymentStatus;
import com.shopsphere.service.PaymentService;
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
import org.springframework.web.bind.annotation.*;

/**
 * Payment recording and status management for an order. Creation and status
 * queries are available to any authenticated shopper (their own orders);
 * status transitions (e.g. confirming a gateway callback) are restricted to
 * ROLE_ADMIN, since customers should never be able to mark their own payment
 * "SUCCESS" client-side.
 */
@Slf4j
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment recording, status tracking, and admin status updates")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Record a payment for an order", description = "Amount must match the order total exactly.")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Recording payment for orderId={}", request.getOrderId());
        PaymentResponse response = paymentService.savePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully", response));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get full payment details for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentDetails(
            @Parameter(description = "Order id") @PathVariable Long orderId) {
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/order/{orderId}/status")
    @Operation(summary = "Get just the payment status for an order", description = "Lightweight endpoint for polling payment confirmation.")
    public ResponseEntity<ApiResponse<PaymentStatus>> getPaymentStatus(
            @Parameter(description = "Order id") @PathVariable Long orderId) {
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(response.getStatus()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a payment's status (admin only)", description = "Also transitions the associated order's status (e.g. SUCCESS confirms the order).")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePaymentStatus(
            @Parameter(description = "Payment id") @PathVariable Long id,
            @Parameter(description = "New payment status") @RequestParam PaymentStatus status) {
        log.info("Updating paymentId={} to status={}", id, status);
        PaymentResponse response = paymentService.updatePaymentStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully", response));
    }
}
