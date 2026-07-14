package com.shopsphere.service;

import com.shopsphere.dto.request.ProductRequest;
import com.shopsphere.dto.response.PaginationResponse;
import com.shopsphere.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(Long sellerId, ProductRequest request);

    ProductResponse updateProduct(Long productId, Long sellerId, ProductRequest request);

    void deleteProduct(Long productId, Long sellerId);

    ProductResponse getProductById(Long id);

    PaginationResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDirection);

    PaginationResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size, String sortBy, String sortDirection);

    PaginationResponse<ProductResponse> searchProducts(String keyword, int page, int size, String sortBy, String sortDirection);

    List<ProductResponse> getLatestProducts();

    PaginationResponse<ProductResponse> getProductsBySeller(Long sellerId, int page, int size, String sortBy, String sortDirection);
}
