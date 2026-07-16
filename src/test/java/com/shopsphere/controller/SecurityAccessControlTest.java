package com.shopsphere.controller;

import com.shopsphere.config.SecurityConfig;
import com.shopsphere.dto.response.PaginationResponse;
import com.shopsphere.dto.response.ProductResponse;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.RoleName;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.security.CustomUserDetailsService;
import com.shopsphere.security.JwtAccessDeniedHandler;
import com.shopsphere.security.JwtAuthenticationEntryPoint;
import com.shopsphere.security.JwtAuthenticationFilter;
import com.shopsphere.security.JwtService;
import com.shopsphere.service.CartService;
import com.shopsphere.service.ProductService;
import com.shopsphere.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the REAL {@link SecurityConfig} filter chain end-to-end
 * (unlike the other controller tests, which disable filters to focus on
 * request/response mapping). Verifies:
 * <ul>
 *   <li>Public GET endpoints are reachable with no Authorization header</li>
 *   <li>Protected endpoints reject requests with no token (401)</li>
 *   <li>Protected endpoints reject requests with a valid token but the wrong role (403)</li>
 *   <li>Protected endpoints accept requests with a valid token and the right role (200)</li>
 * </ul>
 * {@link JwtService} and {@link CustomUserDetailsService} are mocked so no
 * real signing key or database is needed - only the authorization DECISION
 * logic in SecurityConfig/JwtAuthenticationFilter is under test here.
 */
@WebMvcTest(controllers = {ProductController.class, CartController.class, UserController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class SecurityAccessControlTest {

    private static final String VALID_CUSTOMER_TOKEN = "valid-customer-token";
    private static final String VALID_ADMIN_TOKEN = "valid-admin-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private ProductService productService;

    @MockBean
    private CartService cartService;

    @MockBean
    private UserService userService;

    @Test
    void publicProductBrowsing_shouldBeAccessible_withoutAnyToken() throws Exception {
        PaginationResponse<ProductResponse> page = PaginationResponse.<ProductResponse>builder()
                .content(List.of())
                .pageNumber(0).pageSize(10).totalElements(0).totalPages(0).first(true).last(true)
                .build();
        when(productService.getAllProducts(0, 10, "id", "asc")).thenReturn(page);

        mockMvc.perform(get("/v1/products").param("page", "0").param("size", "10")
                        .param("sortBy", "id").param("sortDirection", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedCartEndpoint_shouldReturn401_withNoToken() throws Exception {
        mockMvc.perform(get("/v1/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedCartEndpoint_shouldReturn401_withMalformedBearerHeader() throws Exception {
        when(jwtService.extractUsername("garbage-token")).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad token"));

        mockMvc.perform(get("/v1/cart").header("Authorization", "Bearer garbage-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedCartEndpoint_shouldReturn200_withValidCustomerToken() throws Exception {
        CustomUserDetails customer = customerPrincipal();

        when(jwtService.extractUsername(VALID_CUSTOMER_TOKEN)).thenReturn("customer_amit");
        when(userDetailsService.loadUserByUsername("customer_amit")).thenReturn(customer);
        when(jwtService.isAccessTokenValid(VALID_CUSTOMER_TOKEN, customer)).thenReturn(true);
        when(cartService.getCart(1L)).thenReturn(com.shopsphere.dto.response.CartResponse.builder()
                .id(100L).userId(1L).items(List.of()).build());

        mockMvc.perform(get("/v1/cart").header("Authorization", "Bearer " + VALID_CUSTOMER_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void adminOnlyUserList_shouldReturn403_whenCallerHasCustomerRole() throws Exception {
        CustomUserDetails customer = customerPrincipal();

        when(jwtService.extractUsername(VALID_CUSTOMER_TOKEN)).thenReturn("customer_amit");
        when(userDetailsService.loadUserByUsername("customer_amit")).thenReturn(customer);
        when(jwtService.isAccessTokenValid(VALID_CUSTOMER_TOKEN, customer)).thenReturn(true);

        mockMvc.perform(get("/v1/users").header("Authorization", "Bearer " + VALID_CUSTOMER_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnlyUserList_shouldReturn200_whenCallerHasAdminRole() throws Exception {
        CustomUserDetails admin = adminPrincipal();

        when(jwtService.extractUsername(VALID_ADMIN_TOKEN)).thenReturn("admin_john");
        when(userDetailsService.loadUserByUsername("admin_john")).thenReturn(admin);
        when(jwtService.isAccessTokenValid(VALID_ADMIN_TOKEN, admin)).thenReturn(true);
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/v1/users").header("Authorization", "Bearer " + VALID_ADMIN_TOKEN))
                .andExpect(status().isOk());
    }

    private CustomUserDetails customerPrincipal() {
        Role role = Role.builder().id(3L).name(RoleName.ROLE_CUSTOMER).build();
        User user = User.builder()
                .id(1L).username("customer_amit").email("customer@shopsphere.com")
                .password("hashed").enabled(true).accountNonLocked(true)
                .roles(Set.of(role)).build();
        return new CustomUserDetails(user);
    }

    private CustomUserDetails adminPrincipal() {
        Role role = Role.builder().id(1L).name(RoleName.ROLE_ADMIN).build();
        User user = User.builder()
                .id(99L).username("admin_john").email("admin@shopsphere.com")
                .password("hashed").enabled(true).accountNonLocked(true)
                .roles(Set.of(role)).build();
        return new CustomUserDetails(user);
    }
}
