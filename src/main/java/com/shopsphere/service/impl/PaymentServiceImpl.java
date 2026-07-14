package com.shopsphere.service.impl;

import com.shopsphere.dto.request.PaymentRequest;
import com.shopsphere.dto.response.PaymentResponse;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.PaymentStatus;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.PaymentFailedException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.mapper.PaymentMapper;
import com.shopsphere.repository.OrderRepository;
import com.shopsphere.repository.PaymentRepository;
import com.shopsphere.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse savePayment(PaymentRequest request) {
        log.info("Saving payment for orderId={}", request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        if (paymentRepository.findByOrderId(order.getId()).isPresent()) {
            throw new BadRequestException("A payment record already exists for order '" + order.getOrderNumber() + "'.");
        }

        if (request.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new PaymentFailedException(
                    "Payment amount (" + request.getAmount() + ") does not match the order total (" + order.getTotalAmount() + ").");
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .transactionId(request.getTransactionId())
                .amount(request.getAmount())
                .status(request.getTransactionId() != null ? PaymentStatus.SUCCESS : PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);

        // A successful payment automatically confirms the order.
        if (saved.getStatus() == PaymentStatus.SUCCESS) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }

        log.info("Payment saved: id={}, status={}, orderId={}", saved.getId(), saved.getStatus(), order.getId());

        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(Long paymentId, PaymentStatus status) {
        log.info("Updating paymentId={} to status={}", paymentId, status);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        payment.setStatus(status);
        Payment updated = paymentRepository.save(payment);

        Order order = updated.getOrder();
        switch (status) {
            case SUCCESS -> order.setStatus(OrderStatus.CONFIRMED);
            case FAILED, CANCELLED -> order.setStatus(OrderStatus.CANCELLED);
            case REFUNDED -> order.setStatus(OrderStatus.REFUNDED);
            default -> { /* PENDING: leave order status untouched */ }
        }
        orderRepository.save(order);

        log.info("Payment status updated: id={}, status={}", paymentId, status);

        return paymentMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        return paymentMapper.toResponse(payment);
    }
}
