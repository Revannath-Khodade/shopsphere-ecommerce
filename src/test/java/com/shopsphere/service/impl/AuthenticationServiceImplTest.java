package com.shopsphere.service.impl;

import com.shopsphere.dto.request.AuthenticationRequest;
import com.shopsphere.dto.request.LogoutRequest;
import com.shopsphere.dto.request.RefreshTokenRequest;
import com.shopsphere.dto.request.RegisterRequest;
import com.shopsphere.dto.response.AuthenticationResponse;
import com.shopsphere.dto.response.UserResponse;
import com.shopsphere.entity.RefreshToken;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.RoleName;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.UnauthorizedException;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.RoleRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.security.JwtService;
import com.shopsphere.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private RegisterRequest registerRequest;
    private Role customerRole;
    private User persistedUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("amit_verma")
                .email("amit@example.com")
                .password("Password123!")
                .firstName("Amit")
                .lastName("Verma")
                .phone("+91-9000000000")
                .build();

        customerRole = Role.builder().id(3L).name(RoleName.ROLE_CUSTOMER).build();

        persistedUser = User.builder()
                .id(10L)
                .username("amit_verma")
                .email("amit@example.com")
                .password("hashed-password")
                .enabled(true)
                .accountNonLocked(true)
                .build();

        lenient().when(userMapper.toResponse(any(User.class))).thenReturn(mock(UserResponse.class));
        lenient().when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token");
        lenient().when(jwtService.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh-token");
        lenient().when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);
        lenient().when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        lenient().when(refreshTokenService.createRefreshToken(any(User.class), anyString(), anyLong()))
                .thenReturn(RefreshToken.builder().id(1L).token("refresh-token").build());
    }

    @Test
    void register_shouldIssueTokenPair_whenUsernameAndEmailAreUnique() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);

        AuthenticationResponse response = authenticationService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        verify(cartRepository).save(any());
        verify(refreshTokenService).createRefreshToken(persistedUser, "refresh-token", 604800000L);
    }

    @Test
    void register_shouldThrowDuplicateResourceException_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldThrowDuplicateResourceException_whenEmailAlreadyExists() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldIssueTokenPair_whenCredentialsAreValid() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .usernameOrEmail("amit_verma")
                .password("Password123!")
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(null); // credential check passes
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(persistedUser));

        AuthenticationResponse response = authenticationService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_shouldThrowUnauthorizedException_whenCredentialsAreInvalid() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .usernameOrEmail("amit_verma")
                .password("wrong-password")
                .build();

        doThrow(new BadCredentialsException("Bad credentials")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshToken_shouldRotateAndIssueNewTokenPair_whenTokenIsValid() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("old-refresh-token").build();

        RefreshToken storedToken = RefreshToken.builder()
                .id(1L)
                .token("old-refresh-token")
                .user(persistedUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(jwtService.isRefreshTokenStructurallyValid("old-refresh-token")).thenReturn(true);
        when(refreshTokenService.verifyAndGet("old-refresh-token")).thenReturn(storedToken);

        AuthenticationResponse response = authenticationService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token");
        verify(refreshTokenService).revokeToken("old-refresh-token");
    }

    @Test
    void refreshToken_shouldThrowUnauthorizedException_whenTokenFailsStructuralValidation() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("garbage-token").build();

        when(jwtService.isRefreshTokenStructurallyValid("garbage-token")).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenService, never()).verifyAndGet(anyString());
    }

    @Test
    void logout_shouldRevokeRefreshToken() {
        LogoutRequest request = LogoutRequest.builder().refreshToken("refresh-token").build();

        authenticationService.logout(request);

        verify(refreshTokenService).revokeToken("refresh-token");
    }
}
