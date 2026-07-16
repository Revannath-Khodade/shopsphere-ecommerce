package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.request.ProductRequest;
import com.shopsphere.dto.response.PaginationResponse;
import com.shopsphere.dto.response.ProductResponse;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.RoleName;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc slice test for the product catalog. Security filters are disabled
 * (this test targets controller/service wiring, not the filter chain itself
 * - see SecurityAccessControlTest for filter-chain-level authorization checks),
 * but {@code @AuthenticationPrincipal} resolution is exercised directly via
 * Spring Security Test's {@code user(...)} request post-processor.
 */
@WebMvcTest(controllers = ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private CustomUserDetails sellerPrincipal() {
        Role sellerRole = Role.builder().id(2L).name(RoleName.ROLE_SELLER).build();
        User seller = User.builder()
                .id(2L)
                .username("seller_priya")
                .email("seller@shopsphere.com")
                .password("hashed")
                .enabled(true)
                .accountNonLocked(true)
                .roles(Set.of(sellerRole))
                .build();
        return new CustomUserDetails(seller);
    }

    @Test
    void getProductById_shouldReturn200_withoutAuthentication() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .name("Wireless Bluetooth Headphones")
                .price(new BigDecimal("2999.00"))
                .build();

        when(productService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Wireless Bluetooth Headphones"));
    }

    @Test
    void getAllProducts_shouldReturn200_withDefaultPagination() throws Exception {
        PaginationResponse<ProductResponse> page = PaginationResponse.<ProductResponse>builder()
                .content(java.util.List.of())
                .pageNumber(0)
                .pageSize(10)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();

        when(productService.getAllProducts(anyInt(), anyInt(), anyString(), anyString())).thenReturn(page);

        mockMvc.perform(get("/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNumber").value(0));
    }

    @Test
    void searchProducts_shouldReturn400_whenKeywordIsMissing() throws Exception {
        mockMvc.perform(get("/v1/products/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_shouldReturn201_whenAuthenticatedAsSeller() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Yoga Mat")
                .sku("SKU-TEST-0001")
                .price(new BigDecimal("999.00"))
                .stockQuantity(50)
                .categoryId(1L)
                .build();

        ProductResponse response = ProductResponse.builder().id(10L).name("Yoga Mat").build();

        when(productService.createProduct(eq(2L), any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/products")
                        .with(user(sellerPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Yoga Mat"));
    }

    @Test
    void createProduct_shouldReturn400_whenSkuIsBlank() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Yoga Mat")
                .sku("")
                .price(new BigDecimal("999.00"))
                .stockQuantity(50)
                .categoryId(1L)
                .build();

        mockMvc.perform(post("/v1/products")
                        .with(user(sellerPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
