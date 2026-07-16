package com.shopsphere.controller;

import com.shopsphere.dto.ApiResponse;
import com.shopsphere.dto.request.ProductRequest;
import com.shopsphere.dto.response.PaginationResponse;
import com.shopsphere.dto.response.ProductResponse;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.service.ProductService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product catalog: public read access (browsing/search/filter), seller-only
 * mutations restricted to the products the calling seller actually owns
 * (enforced in {@link com.shopsphere.service.impl.ProductServiceImpl}, not
 * just at the controller layer).
 */
@Slf4j
@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog browsing, search, filtering, and seller management")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a product (seller/admin only)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ProductRequest request) {
        log.info("Creating product sku='{}' by sellerId={}", request.getSku(), currentUser.getId());
        ProductResponse response = productService.createProduct(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a product owned by the calling seller")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Product id") @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        log.info("Updating productId={} by sellerId={}", id, currentUser.getId());
        ProductResponse response = productService.updateProduct(id, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a product owned by the calling seller")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Product id") @PathVariable Long id) {
        log.info("Deleting productId={} by sellerId={}", id, currentUser.getId());
        productService.deleteProduct(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.message("Product deleted successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "Product id") @PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List all active products", description = "Paginated and sortable catalog listing.")
    public ResponseEntity<ApiResponse<PaginationResponse<ProductResponse>>> getAllProducts(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDirection) {
        PaginationResponse<ProductResponse> response = productService.getAllProducts(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by keyword", description = "Matches against product name and description.")
    public ResponseEntity<ApiResponse<PaginationResponse<ProductResponse>>> searchProducts(
            @Parameter(description = "Search keyword") @RequestParam String keyword,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDirection) {
        PaginationResponse<ProductResponse> response = productService.searchProducts(keyword, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter products", description = "Multi-criteria filter by category, brand, and/or price range - every parameter is optional.")
    public ResponseEntity<ApiResponse<PaginationResponse<ProductResponse>>> filterProducts(
            @Parameter(description = "Category id") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Brand name (exact match, case-insensitive)") @RequestParam(required = false) String brand,
            @Parameter(description = "Minimum price (inclusive)") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price (inclusive)") @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDirection) {
        PaginationResponse<ProductResponse> response =
                productService.filterProducts(categoryId, brand, minPrice, maxPrice, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "List products within a category")
    public ResponseEntity<ApiResponse<PaginationResponse<ProductResponse>>> getProductsByCategory(
            @Parameter(description = "Category id") @PathVariable Long categoryId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDirection) {
        PaginationResponse<ProductResponse> response =
                productService.getProductsByCategory(categoryId, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "List products listed by a specific seller")
    public ResponseEntity<ApiResponse<PaginationResponse<ProductResponse>>> getProductsBySeller(
            @Parameter(description = "Seller (user) id") @PathVariable Long sellerId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDirection) {
        PaginationResponse<ProductResponse> response =
                productService.getProductsBySeller(sellerId, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/latest")
    @Operation(summary = "List the 10 most recently added products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLatestProducts() {
        List<ProductResponse> response = productService.getLatestProducts();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
