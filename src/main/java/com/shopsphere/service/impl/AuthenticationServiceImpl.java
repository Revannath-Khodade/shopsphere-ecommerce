package com.shopsphere.service.impl;

import com.shopsphere.dto.request.AuthenticationRequest;
import com.shopsphere.dto.request.LogoutRequest;
import com.shopsphere.dto.request.RefreshTokenRequest;
import com.shopsphere.dto.request.RegisterRequest;
import com.shopsphere.dto.response.AuthenticationResponse;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.RefreshToken;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.RoleName;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.exception.UnauthorizedException;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.RoleRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.security.JwtService;
import com.shopsphere.service.AuthenticationService;
import com.shopsphere.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Implements the full authentication flow: registration, login (delegated to
 * Spring Security's {@link AuthenticationManager}), access/refresh token
 * issuance via {@link JwtService}, refresh-token rotation, and logout
 * (refresh token revocation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Registering new user with username='{}', email='{}'", request.getUsername(), request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.ROLE_CUSTOMER));

        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .enabled(true)
                .accountNonLocked(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);

        // Every new customer gets an empty cart provisioned immediately, so
        // downstream cart operations never have to worry about a missing cart.
        Cart cart = Cart.builder().user(savedUser).build();
        cartRepository.save(cart);

        log.info("User registered successfully: id={}, username='{}'", savedUser.getId(), savedUser.getUsername());

        return issueTokenPair(savedUser);
    }

    @Override
    @Transactional
    public AuthenticationResponse login(AuthenticationRequest request) {
        log.info("Login attempt for identifier='{}'", request.getUsernameOrEmail());

        // Delegates credential verification to Spring Security's
        // AuthenticationManager -> DaoAuthenticationProvider -> our
        // CustomUserDetailsService + PasswordEncoder. This keeps password
        // comparison logic in exactly one place (the framework), rather than
        // duplicating BCrypt matching here.
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            log.warn("Login failed - bad credentials for identifier='{}'", request.getUsernameOrEmail());
            throw new UnauthorizedException("Invalid username/email or password");
        } catch (DisabledException | LockedException e) {
            log.warn("Login rejected - account disabled/locked for identifier='{}'", request.getUsernameOrEmail());
            throw new UnauthorizedException("This account is disabled or locked. Please contact support.");
        } catch (AuthenticationException e) {
            log.warn("Login failed for identifier='{}': {}", request.getUsernameOrEmail(), e.getMessage());
            throw new UnauthorizedException("Invalid username/email or password");
        }

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid username/email or password"));

        log.info("User logged in successfully: username='{}'", user.getUsername());

        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken();
        log.info("Processing refresh token request");

        if (!jwtService.isRefreshTokenStructurallyValid(rawToken)) {
            log.warn("Refresh token failed structural validation (bad signature, wrong type, or expired)");
            throw new UnauthorizedException("Invalid or expired refresh token. Please log in again.");
        }

        // Structural validity is necessary but not sufficient - the token must
        // also still exist server-side and not have been revoked (e.g. by a
        // prior logout or a previous refresh that already rotated it out).
        RefreshToken storedToken = refreshTokenService.verifyAndGet(rawToken);
        User user = storedToken.getUser();

        // Rotate: revoke the presented token and issue a brand new pair. This
        // limits the blast radius if a refresh token is ever stolen - it can
        // only be used once before rotation invalidates it.
        refreshTokenService.revokeToken(rawToken);

        log.info("Refresh token rotated successfully for userId={}", user.getId());

        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        log.info("Processing logout request");
        refreshTokenService.revokeToken(request.getRefreshToken());
        log.info("Logout complete - refresh token revoked");
    }

    /**
     * Builds a {@link CustomUserDetails} view of the user, issues a signed
     * access token + refresh token pair, persists the refresh token, and
     * assembles the full {@link AuthenticationResponse}.
     */
    private AuthenticationResponse issueTokenPair(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshTokenValue = jwtService.generateRefreshToken(userDetails);

        refreshTokenService.createRefreshToken(user, refreshTokenValue, jwtService.getRefreshTokenExpirationMs());

        return AuthenticationResponse.builder()
                .token(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType(TOKEN_TYPE)
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .user(userMapper.toResponse(user))
                .build();
    }
}
