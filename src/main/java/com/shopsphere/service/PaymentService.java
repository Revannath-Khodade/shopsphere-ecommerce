package com.shopsphere.service;

import com.shopsphere.dto.request.PaymentRequest;
import com.shopsphere.dto.response.PaymentResponse;
import com.shopsphere.entity.enums.PaymentStatus;

public interface PaymentService {

    PaymentResponse savePayment(PaymentRequest request);

    PaymentResponse updatePaymentStatus(Long paymentId, PaymentStatus status);

    PaymentResponse getPaymentByOrderId(Long orderId);
}
