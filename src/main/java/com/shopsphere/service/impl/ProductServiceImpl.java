package com.shopsphere.service.impl;

import com.shopsphere.dto.request.ProductRequest;
import com.shopsphere.dto.response.PaginationResponse;
import com.shopsphere.dto.response.ProductResponse;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.ProductImage;
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
import com.shopsphere.service.ProductService;
import com.shopsphere.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(Long sellerId, ProductRequest request) {
        log.info("Creating product sku='{}' for sellerId={}", request.getSku(), sellerId);

        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", sellerId));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        validatePricing(request);

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sku(request.getSku())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .stockQuantity(request.getStockQuantity())
                .brand(request.getBrand())
                .active(request.getActive() == null || request.getActive())
                .category(category)
                .seller(seller)
                .build();

        attachImages(product, request.getImageUrls());

        Product saved = productRepository.save(product);
        log.info("Product created: id={}, sku='{}'", saved.getId(), saved.getSku());

        return enrichWithRating(productMapper.toResponse(saved));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, Long sellerId, ProductRequest request) {
        log.info("Updating productId={} by sellerId={}", productId, sellerId);

        Product product = findProductOrThrow(productId);
        assertOwnership(product, sellerId);

        if (!product.getSku().equalsIgnoreCase(request.getSku())
                && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        validatePricing(request);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setBrand(request.getBrand());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        product.setCategory(category);

        if (request.getImageUrls() != null) {
            product.getImages().clear();
            attachImages(product, request.getImageUrls());
        }

        Product updated = productRepository.save(product);
        log.info("Product updated: id={}", updated.getId());

        return enrichWithRating(productMapper.toResponse(updated));
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId, Long sellerId) {
        log.info("Deleting productId={} by sellerId={}", productId, sellerId);
        Product product = findProductOrThrow(productId);
        assertOwnership(product, sellerId);
        productRepository.delete(product);
        log.info("Product deleted: id={}", productId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return enrichWithRating(productMapper.toResponse(findProductOrThrow(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, sortDirection);
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);
        return toPaginationResponse(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size, String sortBy, String sortDirection) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }
        Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, sortDirection);
        Page<Product> productPage = productRepository.findByCategoryId(categoryId, pageable);
        return toPaginationResponse(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ProductResponse> searchProducts(String keyword, int page, int size, String sortBy, String sortDirection) {
        if (keyword == null || keyword.isBlank()) {
            throw new BadRequestException("Search keyword must not be blank");
        }
        Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, sortDirection);
        Page<Product> productPage = productRepository.searchByKeyword(keyword.trim(), pageable);
        return toPaginationResponse(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLatestProducts() {
        return productRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(productMapper::toResponse)
                .map(this::enrichWithRating)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ProductResponse> getProductsBySeller(Long sellerId, int page, int size, String sortBy, String sortDirection) {
        if (!userRepository.existsById(sellerId)) {
            throw new ResourceNotFoundException("User", "id", sellerId);
        }
        Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, sortDirection);
        Page<Product> productPage = productRepository.findBySellerId(sellerId, pageable);
        return toPaginationResponse(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ProductResponse> filterProducts(Long categoryId, String brand, BigDecimal minPrice,
                                                                BigDecimal maxPrice, int page, int size,
                                                                String sortBy, String sortDirection) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must not be greater than maxPrice.");
        }
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }

        Pageable pageable = PaginationUtil.buildPageable(page, size, sortBy, sortDirection);
        Page<Product> productPage = productRepository.filterProducts(categoryId, brand, minPrice, maxPrice, pageable);
        return toPaginationResponse(productPage);
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    /** Ensures only the seller who listed a product may modify or delete it. */
    private void assertOwnership(Product product, Long sellerId) {
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenException("You do not have permission to modify this product.");
        }
    }

    private void validatePricing(ProductRequest request) {
        if (request.getDiscountPrice() != null
                && request.getDiscountPrice().compareTo(request.getPrice()) > 0) {
            throw new BadRequestException("Discount price cannot be greater than the regular price.");
        }
    }

    private void attachImages(Product product, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(ProductImage.builder()
                    .imageUrl(imageUrls.get(i))
                    .isPrimary(i == 0)
                    .displayOrder(i)
                    .product(product)
                    .build());
        }
        product.getImages().addAll(images);
    }

    /** Enriches a mapped ProductResponse with average rating / review count from the review table. */
    private ProductResponse enrichWithRating(ProductResponse response) {
        Double avgRating = reviewRepository.findAverageRatingByProductId(response.getId());
        long reviewCount = reviewRepository.findByProductId(response.getId(), Pageable.unpaged()).getTotalElements();
        response.setAverageRating(avgRating == null ? 0.0 : Math.round(avgRating * 10.0) / 10.0);
        response.setReviewCount(reviewCount);
        return response;
    }

    private PaginationResponse<ProductResponse> toPaginationResponse(Page<Product> productPage) {
        List<ProductResponse> content = productPage.getContent().stream()
                .map(productMapper::toResponse)
                .map(this::enrichWithRating)
                .toList();
        return PaginationResponse.from(productPage, content);
    }
}
