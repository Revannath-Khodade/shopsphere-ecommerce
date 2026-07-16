package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.request.CartRequest;
import com.shopsphere.dto.response.CartResponse;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.RoleName;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.service.CartService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    private CustomUserDetails customerPrincipal() {
        Role customerRole = Role.builder().id(3L).name(RoleName.ROLE_CUSTOMER).build();
        User customer = User.builder()
                .id(1L)
                .username("customer_amit")
                .email("customer@shopsphere.com")
                .password("hashed")
                .enabled(true)
                .accountNonLocked(true)
                .roles(Set.of(customerRole))
                .build();
        return new CustomUserDetails(customer);
    }

    @Test
    void getCart_shouldReturn200_andScopeToAuthenticatedUser() throws Exception {
        CartResponse response = CartResponse.builder()
                .id(100L)
                .userId(1L)
                .items(java.util.List.of())
                .totalItems(0)
                .totalAmount(BigDecimal.ZERO)
                .build();

        when(cartService.getCart(1L)).thenReturn(response);

        mockMvc.perform(get("/v1/cart").with(user(customerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    void addItem_shouldReturn200_whenRequestIsValid() throws Exception {
        CartRequest request = CartRequest.builder().productId(1L).quantity(2).build();
        CartResponse response = CartResponse.builder().id(100L).userId(1L).items(java.util.List.of()).build();

        when(cartService.addItem(eq(1L), any(CartRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/cart/items")
                        .with(user(customerPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void addItem_shouldReturn400_whenQuantityIsZero() throws Exception {
        CartRequest request = CartRequest.builder().productId(1L).quantity(0).build();

        mockMvc.perform(post("/v1/cart/items")
                        .with(user(customerPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItemQuantity_shouldReturn400_whenQuantityIsNegative() throws Exception {
        mockMvc.perform(put("/v1/cart/items/1")
                        .with(user(customerPrincipal()))
                        .param("quantity", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clearCart_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/v1/cart").with(user(customerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared successfully"));
    }
}
