package com.shopsphere.controller;

import com.shopsphere.dto.ApiResponse;
import com.shopsphere.dto.request.ReviewRequest;
import com.shopsphere.dto.response.PaginationResponse;
import com.shopsphere.dto.response.ReviewResponse;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.service.ReviewService;
import com.shopsphere.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Product reviews. Reading a product's reviews and average rating is public
 * (see SecurityConfig's PUBLIC_GET_ENDPOINTS); writing a review requires
 * authentication and a verified purchase (enforced in ReviewServiceImpl).
 */
@Slf4j
@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews and ratings")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a review for a product", description = "Requires a verified purchase; one review per product per user.")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                   @Valid @RequestBody ReviewRequest request) {
        log.info("Adding review for productId={} by userId={}", request.getProductId(), currentUser.getId());
        ReviewResponse response = reviewService.addReview(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review added successfully", response));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update the current user's own review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                      @Parameter(description = "Review id") @PathVariable Long id,
                                                                      @Valid @RequestBody ReviewRequest request) {
        log.info("Updating reviewId={} by userId={}", id, currentUser.getId());
        ReviewResponse response = reviewService.updateReview(currentUser.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete the current user's own review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                            @Parameter(description = "Review id") @PathVariable Long id) {
        log.info("Deleting reviewId={} by userId={}", id, currentUser.getId());
        reviewService.deleteReview(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.message("Review deleted successfully"));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get paginated reviews for a product")
    public ResponseEntity<ApiResponse<PaginationResponse<ReviewResponse>>> getReviewsForProduct(
            @Parameter(description = "Product id") @PathVariable Long productId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        PaginationResponse<ReviewResponse> response = reviewService.getReviewsForProduct(productId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/product/{productId}/average-rating")
    @Operation(summary = "Get a product's average rating")
    public ResponseEntity<ApiResponse<Double>> getAverageRating(
            @Parameter(description = "Product id") @PathVariable Long productId) {
        Double response = reviewService.getAverageRating(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
