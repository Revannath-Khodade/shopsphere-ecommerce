package com.shopsphere.service.impl;

import com.shopsphere.dto.request.ProductRequest;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.ForbiddenException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.mapper.ProductMapper;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.ReviewRepository;
import com.shopsphere.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductRequest productRequest;
    private User seller;
    private Category category;

    @BeforeEach
    void setUp() {
        productRequest = ProductRequest.builder()
                .name("Yoga Mat with Carry Strap")
                .description("Extra-thick non-slip yoga mat")
                .sku("SKU-SPRT-0002")
                .price(new BigDecimal("1299.00"))
                .discountPrice(new BigDecimal("999.00"))
                .stockQuantity(400)
                .categoryId(5L)
                .build();

        seller = User.builder().id(2L).firstName("Priya").lastName("Sharma").build();
        category = Category.builder().id(5L).name("Sports & Fitness").build();
    }

    @Test
    void createProduct_shouldThrowDuplicateResourceException_whenSkuAlreadyExists() {
        when(productRepository.existsBySku(productRequest.getSku())).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(2L, productRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createProduct_shouldThrowBadRequestException_whenDiscountPriceExceedsPrice() {
        productRequest.setDiscountPrice(new BigDecimal("5000.00"));

        when(productRepository.existsBySku(productRequest.getSku())).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> productService.createProduct(2L, productRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createProduct_shouldThrowResourceNotFoundException_whenCategoryDoesNotExist() {
        when(productRepository.existsBySku(productRequest.getSku())).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(categoryRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(2L, productRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProduct_shouldThrowForbiddenException_whenSellerDoesNotOwnProduct() {
        Product existingProduct = Product.builder()
                .id(1L)
                .sku("SKU-SPRT-0002")
                .seller(User.builder().id(99L).build()) // a different seller owns it
                .images(new HashSet<>())
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        assertThatThrownBy(() -> productService.updateProduct(1L, 2L, productRequest))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteProduct_shouldThrowForbiddenException_whenSellerDoesNotOwnProduct() {
        Product existingProduct = Product.builder()
                .id(1L)
                .seller(User.builder().id(99L).build())
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        assertThatThrownBy(() -> productService.deleteProduct(1L, 2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getProductById_shouldThrowResourceNotFoundException_whenProductDoesNotExist() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
