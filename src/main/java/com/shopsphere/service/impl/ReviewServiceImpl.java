package com.shopsphere.service.impl;

import com.shopsphere.dto.request.ReviewRequest;
import com.shopsphere.dto.response.PaginationResponse;
import com.shopsphere.dto.response.ReviewResponse;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.Review;
import com.shopsphere.entity.User;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.ForbiddenException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.mapper.ReviewMapper;
import com.shopsphere.repository.OrderItemRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.ReviewRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.ReviewService;
import com.shopsphere.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponse addReview(Long userId, ReviewRequest request) {
        log.info("Adding review for productId={} by userId={}", request.getProductId(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (reviewRepository.existsByUserIdAndProductId(userId, product.getId())) {
            throw new DuplicateResourceException("You have already reviewed this product. Please edit your existing review instead.");
        }

        // Verified-purchase rule: a customer may only review products they have actually bought.
        boolean hasPurchased = orderItemRepository.findByProductId(product.getId()).stream()
                .anyMatch(item -> item.getOrder().getUser().getId().equals(userId));
        if (!hasPurchased) {
            throw new BadRequestException("You can only review products you have purchased.");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Review added: id={}, productId={}, rating={}", saved.getId(), product.getId(), saved.getRating());

        return reviewMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request) {
        log.info("Updating reviewId={} by userId={}", reviewId, userId);

        Review review = findReviewOrThrow(reviewId);
        assertOwnership(review, userId);

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review updated = reviewRepository.save(review);
        log.info("Review updated: id={}", updated.getId());

        return reviewMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        log.info("Deleting reviewId={} by userId={}", reviewId, userId);

        Review review = findReviewOrThrow(reviewId);
        assertOwnership(review, userId);

        reviewRepository.delete(review);
        log.info("Review deleted: id={}", reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ReviewResponse> getReviewsForProduct(Long productId, int page, int size) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        Pageable pageable = PaginationUtil.buildPageable(page, size, "createdAt", "desc");
        Page<Review> reviewPage = reviewRepository.findByProductId(productId, pageable);
        List<ReviewResponse> content = reviewPage.getContent().stream()
                .map(reviewMapper::toResponse)
                .toList();
        return PaginationResponse.from(reviewPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        Double average = reviewRepository.findAverageRatingByProductId(productId);
        return average == null ? 0.0 : Math.round(average * 10.0) / 10.0;
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private Review findReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
    }

    private void assertOwnership(Review review, Long userId) {
        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to modify this review.");
        }
    }
}
