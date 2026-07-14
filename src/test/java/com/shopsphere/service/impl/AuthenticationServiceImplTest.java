package com.shopsphere.service.impl;

import com.shopsphere.dto.request.AuthenticationRequest;
import com.shopsphere.dto.request.RegisterRequest;
import com.shopsphere.dto.response.AuthenticationResponse;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.RoleName;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.UnauthorizedException;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.RoleRepository;
import com.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private RegisterRequest registerRequest;
    private Role customerRole;

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
    }

    @Test
    void register_shouldCreateUserWithCustomerRole_whenUsernameAndEmailAreUnique() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed-password");

        User savedUser = User.builder()
                .id(10L)
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password("hashed-password")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(mock(com.shopsphere.dto.response.UserResponse.class));

        AuthenticationResponse response = authenticationService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(cartRepository).save(any());
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
    void login_shouldReturnAuthenticationResponse_whenCredentialsAreValid() {
        User user = User.builder()
                .id(10L)
                .username("amit_verma")
                .password("hashed-password")
                .enabled(true)
                .accountNonLocked(true)
                .build();

        AuthenticationRequest request = AuthenticationRequest.builder()
                .usernameOrEmail("amit_verma")
                .password("Password123!")
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(userMapper.toResponse(user)).thenReturn(mock(com.shopsphere.dto.response.UserResponse.class));

        AuthenticationResponse response = authenticationService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
    }

    @Test
    void login_shouldThrowUnauthorizedException_whenPasswordDoesNotMatch() {
        User user = User.builder()
                .id(10L)
                .username("amit_verma")
                .password("hashed-password")
                .enabled(true)
                .accountNonLocked(true)
                .build();

        AuthenticationRequest request = AuthenticationRequest.builder()
                .usernameOrEmail("amit_verma")
                .password("wrong-password")
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_shouldThrowUnauthorizedException_whenUserDoesNotExist() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .usernameOrEmail("unknown_user")
                .password("Password123!")
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }
}
