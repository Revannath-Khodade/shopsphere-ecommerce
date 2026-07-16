package com.shopsphere.service.impl;

import com.shopsphere.dto.request.OrderRequest;
import com.shopsphere.dto.response.OrderResponse;
import com.shopsphere.dto.response.PaginationResponse;
import com.shopsphere.entity.Address;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.CartItem;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.OrderItem;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.PaymentStatus;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.CartEmptyException;
import com.shopsphere.exception.ForbiddenException;
import com.shopsphere.exception.OrderNotFoundException;
import com.shopsphere.exception.OutOfStockException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.mapper.OrderMapper;
import com.shopsphere.repository.AddressRepository;
import com.shopsphere.repository.CartItemRepository;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.OrderRepository;
import com.shopsphere.repository.PaymentRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.OrderService;
import com.shopsphere.util.OrderNumberGenerator;
import com.shopsphere.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** Order states from which a customer is still allowed to cancel. */
    private static final Set<OrderStatus> CANCELLABLE_STATUSES =
            Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest request) {
        log.info("Placing order for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new CartEmptyException();
        }

        Address shippingAddress = findOwnedAddress(request.getShippingAddressId(), userId);
        Address billingAddress = findOwnedAddress(request.getBillingAddressId(), userId);

        // Re-validate stock for every line item at checkout time - stock may have
        // changed since items were added to the cart.
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() == null || product.getStockQuantity() < cartItem.getQuantity()) {
                int available = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                throw new OutOfStockException(product.getName(), cartItem.getQuantity(), available);
            }
        }

        Order order = Order.builder()
                .orderNumber(OrderNumberGenerator.generate())
                .status(OrderStatus.PENDING)
                .shippingFee(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .notes(request.getNotes())
                .user(user)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .build();

        Set<OrderItem> orderItems = new HashSet<>();
        BigDecimal subtotalSum = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            BigDecimal lineSubtotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .subtotal(lineSubtotal)
                    .build();
            orderItems.add(orderItem);
            subtotalSum = subtotalSum.add(lineSubtotal);

            // Decrement stock now that the order is confirmed to be placeable.
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(subtotalSum.add(order.getShippingFee()).add(order.getTaxAmount()));

        // A pending payment record is created alongside the order; PaymentService
        // (or a payment gateway callback) later transitions it to SUCCESS/FAILED.
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);

        // Cart is emptied only after the order has been successfully persisted.
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getCartItems().clear();

        log.info("Order placed successfully: orderNumber='{}', userId={}, total={}",
                savedOrder.getOrderNumber(), userId, savedOrder.getTotalAmount());

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        log.info("Cancelling orderId={} for userId={}", orderId, userId);

        Order order = findOrderOrThrow(orderId);
        assertOwnership(order, userId);

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new BadRequestException(
                    "Order '" + order.getOrderNumber() + "' cannot be cancelled because it is already " + order.getStatus() + ".");
        }

        // Restock every item before flipping the order status.
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        if (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.SUCCESS) {
            order.getPayment().setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(order.getPayment());
        }

        Order cancelled = orderRepository.save(order);
        log.info("Order cancelled: orderNumber='{}'", cancelled.getOrderNumber());

        return orderMapper.toResponse(cancelled);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<OrderResponse> getOrderHistory(Long userId, int page, int size) {
        Pageable pageable = PaginationUtil.buildPageable(page, size, "orderDate", "desc");
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        List<OrderResponse> content = orderPage.getContent().stream()
                .map(orderMapper::toResponse)
                .toList();
        return PaginationResponse.from(orderPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetails(Long userId, Long orderId) {
        Order order = findOrderOrThrow(orderId);
        assertOwnership(order, userId);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<OrderResponse> getAllOrders(OrderStatus status, int page, int size) {
        Pageable pageable = PaginationUtil.buildPageable(page, size, "orderDate", "desc");
        Page<Order> orderPage = status != null
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        List<OrderResponse> content = orderPage.getContent().stream()
                .map(orderMapper::toResponse)
                .toList();
        return PaginationResponse.from(orderPage, content);
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    private void assertOwnership(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to access this order.");
        }
    }

    private Address findOwnedAddress(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        if (!address.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to use this address.");
        }
        return address;
    }
}
