package com.shopsphere.dto.request;

import com.shopsphere.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Checkout payload. The order's line items are derived server-side from the
 * customer's current cart contents (never trusted from the client) - only
 * the addresses and payment method are supplied here.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull(message = "Shipping address is required")
    private Long shippingAddressId;

    @NotNull(message = "Billing address is required")
    private Long billingAddressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
