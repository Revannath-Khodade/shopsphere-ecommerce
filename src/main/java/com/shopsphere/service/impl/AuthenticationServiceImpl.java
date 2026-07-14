package com.shopsphere.service.impl;

import com.shopsphere.dto.request.AuthenticationRequest;
import com.shopsphere.dto.request.RegisterRequest;
import com.shopsphere.dto.response.AuthenticationResponse;
import com.shopsphere.entity.Cart;
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
import com.shopsphere.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Implements registration and login business logic.
 * <p>
 * Token generation here is a placeholder opaque UUID - real JWT issuance is
 * wired up in the dedicated Security phase by swapping out
 * {@link #issuePlaceholderToken(User)} for a call into a JwtService. Nothing
 * else in this class (or its callers) needs to change when that happens.
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

        return AuthenticationResponse.builder()
                .token(issuePlaceholderToken(savedUser))
                .tokenType(TOKEN_TYPE)
                .user(userMapper.toResponse(savedUser))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResponse login(AuthenticationRequest request) {
        log.info("Login attempt for identifier='{}'", request.getUsernameOrEmail());

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - no user found for identifier='{}'", request.getUsernameOrEmail());
                    return new UnauthorizedException("Invalid username/email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - bad credentials for username='{}'", user.getUsername());
            throw new UnauthorizedException("Invalid username/email or password");
        }

        if (!Boolean.TRUE.equals(user.getEnabled()) || !Boolean.TRUE.equals(user.getAccountNonLocked())) {
            log.warn("Login rejected - account disabled or locked for username='{}'", user.getUsername());
            throw new UnauthorizedException("This account is disabled or locked. Please contact support.");
        }

        log.info("User logged in successfully: username='{}'", user.getUsername());

        return AuthenticationResponse.builder()
                .token(issuePlaceholderToken(user))
                .tokenType(TOKEN_TYPE)
                .user(userMapper.toResponse(user))
                .build();
    }

    /**
     * Placeholder token generator for Phase 2. Produces a random, non-JWT
     * opaque string so the API contract (AuthenticationResponse.token) is
     * already stable ahead of the Security phase's real JWT implementation.
     */
    private String issuePlaceholderToken(User user) {
        return UUID.randomUUID().toString().replace("-", "") + "." + user.getId();
    }
}
