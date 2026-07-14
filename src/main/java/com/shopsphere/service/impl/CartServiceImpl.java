package com.shopsphere.service.impl;

import com.shopsphere.dto.request.CartRequest;
import com.shopsphere.dto.response.CartResponse;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.CartItem;
import com.shopsphere.entity.Product;
import com.shopsphere.exception.OutOfStockException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.mapper.CartMapper;
import com.shopsphere.repository.CartItemRepository;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return cartMapper.toResponse(findCartOrThrow(userId));
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartRequest request) {
        log.info("Adding productId={} (qty={}) to cart of userId={}", request.getProductId(), request.getQuantity(), userId);

        Cart cart = findCartOrThrow(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        int desiredQuantity = (existingItem == null ? 0 : existingItem.getQuantity()) + request.getQuantity();
        validateStock(product, desiredQuantity);

        if (existingItem != null) {
            existingItem.setQuantity(desiredQuantity);
            existingItem.setPrice(effectivePrice(product)); // refresh price snapshot to current price
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .price(effectivePrice(product))
                    .build();
            cart.getCartItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        log.info("Cart updated for userId={}: productId={}, quantity={}", userId, product.getId(), desiredQuantity);

        return cartMapper.toResponse(findCartOrThrow(userId));
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, Integer quantity) {
        log.info("Updating quantity for productId={} in cart of userId={} to {}", productId, userId, quantity);

        Cart cart = findCartOrThrow(userId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId));

        validateStock(item.getProduct(), quantity);

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        log.info("Cart item quantity updated: userId={}, productId={}, quantity={}", userId, productId, quantity);

        return cartMapper.toResponse(findCartOrThrow(userId));
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        log.info("Removing productId={} from cart of userId={}", productId, userId);

        Cart cart = findCartOrThrow(userId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId));

        cart.getCartItems().remove(item);
        cartItemRepository.delete(item);

        log.info("Cart item removed: userId={}, productId={}", userId, productId);

        return cartMapper.toResponse(findCartOrThrow(userId));
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for userId={}", userId);
        Cart cart = findCartOrThrow(userId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getCartItems().clear();
        log.info("Cart cleared for userId={}", userId);
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private Cart findCartOrThrow(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
    }

    private void validateStock(Product product, int requestedQuantity) {
        if (product.getStockQuantity() == null || product.getStockQuantity() < requestedQuantity) {
            int available = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
            throw new OutOfStockException(product.getName(), requestedQuantity, available);
        }
    }

    /** Uses the active discount price when present, otherwise the regular price. */
    private BigDecimal effectivePrice(Product product) {
        return product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
    }
}
