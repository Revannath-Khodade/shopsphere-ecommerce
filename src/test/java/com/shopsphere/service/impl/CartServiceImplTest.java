package com.shopsphere.service.impl;

import com.shopsphere.dto.request.CartRequest;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.CartItem;
import com.shopsphere.entity.Product;
import com.shopsphere.exception.OutOfStockException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.mapper.CartMapper;
import com.shopsphere.repository.CartItemRepository;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Wireless Bluetooth Headphones")
                .price(new BigDecimal("2999.00"))
                .discountPrice(new BigDecimal("2499.00"))
                .stockQuantity(5)
                .build();

        cart = Cart.builder()
                .id(100L)
                .cartItems(new HashSet<>())
                .build();

        lenient().when(cartRepository.findByUserId(anyLong())).thenReturn(Optional.of(cart));
    }

    @Test
    void addItem_shouldThrowOutOfStockException_whenRequestedQuantityExceedsStock() {
        CartRequest request = CartRequest.builder().productId(1L).quantity(10).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(1L, request))
                .isInstanceOf(OutOfStockException.class);
    }

    @Test
    void addItem_shouldThrowResourceNotFoundException_whenProductDoesNotExist() {
        CartRequest request = CartRequest.builder().productId(999L).quantity(1).build();

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addItem_shouldSucceed_whenStockIsSufficient() {
        CartRequest request = CartRequest.builder().productId(1L).quantity(2).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartMapper.toResponse(cart)).thenReturn(com.shopsphere.dto.response.CartResponse.builder().build());

        cartService.addItem(1L, request);

        // No exception means stock validation passed and the item was persisted.
    }

    @Test
    void updateItemQuantity_shouldThrowResourceNotFoundException_whenItemNotInCart() {
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(1L, 5L, 3))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
