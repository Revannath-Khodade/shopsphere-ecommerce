package com.shopsphere.service;

import com.shopsphere.dto.request.AuthenticationRequest;
import com.shopsphere.dto.request.LogoutRequest;
import com.shopsphere.dto.request.RefreshTokenRequest;
import com.shopsphere.dto.request.RegisterRequest;
import com.shopsphere.dto.response.AuthenticationResponse;

public interface AuthenticationService {

    /**
     * Registers a new customer account: validates uniqueness of username/email,
     * encodes the password, persists the user with the default ROLE_CUSTOMER,
     * issues a signed JWT access token + a persisted refresh token, and
     * returns an authentication response.
     */
    AuthenticationResponse register(RegisterRequest request);

    /**
     * Authenticates the supplied credentials via Spring Security's
     * {@link org.springframework.security.authentication.AuthenticationManager},
     * then issues a fresh access token + refresh token pair on success.
     */
    AuthenticationResponse login(AuthenticationRequest request);

    /**
     * Exchanges a still-valid, non-revoked refresh token for a brand new
     * access token (and a rotated refresh token, invalidating the old one).
     */
    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    /**
     * Logs the user out of a single session by revoking the specific
     * refresh token supplied - the corresponding access token remains valid
     * until its own (short) expiry, as is standard for stateless JWTs.
     */
    void logout(LogoutRequest request);
}
