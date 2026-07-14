package com.shopsphere.service.impl;

import com.shopsphere.dto.request.OrderRequest;
import com.shopsphere.entity.Address;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.PaymentMethod;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.CartEmptyException;
import com.shopsphere.exception.ForbiddenException;
import com.shopsphere.mapper.OrderMapper;
import com.shopsphere.repository.AddressRepository;
import com.shopsphere.repository.CartItemRepository;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.OrderRepository;
import com.shopsphere.repository.PaymentRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).build();

        orderRequest = OrderRequest.builder()
                .shippingAddressId(10L)
                .billingAddressId(10L)
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .build();

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void placeOrder_shouldThrowCartEmptyException_whenCartHasNoItems() {
        Cart emptyCart = Cart.builder().id(100L).cartItems(new HashSet<>()).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));

        assertThatThrownBy(() -> orderService.placeOrder(1L, orderRequest))
                .isInstanceOf(CartEmptyException.class);
    }

    @Test
    void cancelOrder_shouldThrowForbiddenException_whenOrderBelongsToAnotherUser() {
        Order order = Order.builder()
                .id(5L)
                .orderNumber("SS-20260710-ABC123")
                .status(OrderStatus.PENDING)
                .user(User.builder().id(999L).build()) // different owner
                .build();

        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 5L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelOrder_shouldThrowBadRequestException_whenOrderAlreadyDelivered() {
        Order order = Order.builder()
                .id(5L)
                .orderNumber("SS-20260710-ABC123")
                .status(OrderStatus.DELIVERED)
                .user(user)
                .build();

        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 5L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void placeOrder_shouldThrowForbiddenException_whenShippingAddressBelongsToAnotherUser() {
        Cart cart = Cart.builder().id(100L).cartItems(new HashSet<>()).build();
        cart.getCartItems().add(com.shopsphere.entity.CartItem.builder()
                .id(1L)
                .quantity(1)
                .price(new java.math.BigDecimal("100.00"))
                .product(com.shopsphere.entity.Product.builder().id(1L).stockQuantity(10).build())
                .build());

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        Address foreignAddress = Address.builder().id(10L).user(User.builder().id(999L).build()).build();
        when(addressRepository.findById(10L)).thenReturn(Optional.of(foreignAddress));

        assertThatThrownBy(() -> orderService.placeOrder(1L, orderRequest))
                .isInstanceOf(ForbiddenException.class);
    }
}
