package com.shopsphere.service;

import com.shopsphere.dto.request.ReviewRequest;
import com.shopsphere.dto.response.PaginationResponse;
import com.shopsphere.dto.response.ReviewResponse;

public interface ReviewService {

    ReviewResponse addReview(Long userId, ReviewRequest request);

    ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request);

    void deleteReview(Long userId, Long reviewId);

    PaginationResponse<ReviewResponse> getReviewsForProduct(Long productId, int page, int size);

    Double getAverageRating(Long productId);
}
