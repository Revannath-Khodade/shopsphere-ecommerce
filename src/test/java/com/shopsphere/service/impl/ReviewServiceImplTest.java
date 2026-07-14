package com.shopsphere.service.impl;

import com.shopsphere.dto.request.ReviewRequest;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.Review;
import com.shopsphere.entity.User;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.ForbiddenException;
import com.shopsphere.mapper.ReviewMapper;
import com.shopsphere.repository.OrderItemRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.ReviewRepository;
import com.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private ReviewRequest reviewRequest;
    private Product product;
    private User user;

    @BeforeEach
    void setUp() {
        reviewRequest = ReviewRequest.builder()
                .productId(1L)
                .rating(5)
                .comment("Excellent product, highly recommended!")
                .build();

        product = Product.builder().id(1L).name("Atomic Habits").build();
        user = User.builder().id(1L).build();
    }

    @Test
    void addReview_shouldThrowDuplicateResourceException_whenUserAlreadyReviewedProduct() {
        when(reviewRepository.existsByUserIdAndProductId(1L, 1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> reviewService.addReview(1L, reviewRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void addReview_shouldThrowBadRequestException_whenUserHasNotPurchasedProduct() {
        when(reviewRepository.existsByUserIdAndProductId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderItemRepository.findByProductId(1L)).thenReturn(List.of()); // no purchase history

        assertThatThrownBy(() -> reviewService.addReview(1L, reviewRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateReview_shouldThrowForbiddenException_whenReviewBelongsToAnotherUser() {
        Review review = Review.builder()
                .id(1L)
                .user(User.builder().id(999L).build())
                .product(product)
                .rating(4)
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(1L, 1L, reviewRequest))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getAverageRating_shouldReturnZero_whenProductHasNoReviews() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findAverageRatingByProductId(1L)).thenReturn(null);

        Double average = reviewService.getAverageRating(1L);

        org.assertj.core.api.Assertions.assertThat(average).isEqualTo(0.0);
    }
}
