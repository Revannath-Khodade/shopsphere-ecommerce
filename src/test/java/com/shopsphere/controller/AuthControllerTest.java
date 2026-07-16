package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.request.AuthenticationRequest;
import com.shopsphere.dto.request.RegisterRequest;
import com.shopsphere.dto.response.AuthenticationResponse;
import com.shopsphere.dto.response.UserResponse;
import com.shopsphere.exception.UnauthorizedException;
import com.shopsphere.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc slice test for the public authentication endpoints. Security
 * filters are disabled here (these endpoints are public anyway) so the test
 * focuses purely on request/response mapping, Bean Validation, and correct
 * delegation to {@link AuthenticationService}.
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void register_shouldReturn201_whenRequestIsValid() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("amit_verma")
                .email("amit@example.com")
                .password("Password123!")
                .firstName("Amit")
                .lastName("Verma")
                .build();

        AuthenticationResponse serviceResponse = AuthenticationResponse.builder()
                .token("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .user(UserResponse.builder().id(1L).username("amit_verma").build())
                .build();

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(jsonPath("$.data.user.username").value("amit_verma"));
    }

    @Test
    void register_shouldReturn400_whenEmailIsMalformed() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("amit_verma")
                .email("not-an-email")
                .password("Password123!")
                .firstName("Amit")
                .lastName("Verma")
                .build();

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void register_shouldReturn400_whenPasswordIsTooWeak() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("amit_verma")
                .email("amit@example.com")
                .password("weak")
                .firstName("Amit")
                .lastName("Verma")
                .build();

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn200_whenCredentialsAreValid() throws Exception {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .usernameOrEmail("amit_verma")
                .password("Password123!")
                .build();

        AuthenticationResponse serviceResponse = AuthenticationResponse.builder()
                .token("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .user(UserResponse.builder().id(1L).username("amit_verma").build())
                .build();

        when(authenticationService.login(any(AuthenticationRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("access-token"));
    }

    @Test
    void login_shouldReturn401_whenCredentialsAreInvalid() throws Exception {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .usernameOrEmail("amit_verma")
                .password("wrong-password")
                .build();

        when(authenticationService.login(any(AuthenticationRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid username/email or password"));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username/email or password"));
    }

    @Test
    void login_shouldReturn400_whenUsernameOrEmailIsBlank() throws Exception {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .usernameOrEmail("")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
