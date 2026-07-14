package com.shopsphere.service;

import com.shopsphere.dto.request.AuthenticationRequest;
import com.shopsphere.dto.request.RegisterRequest;
import com.shopsphere.dto.response.AuthenticationResponse;

public interface AuthenticationService {

    /**
     * Registers a new customer account: validates uniqueness of username/email,
     * encodes the password, persists the user with the default ROLE_CUSTOMER,
     * and returns an authentication response (mirrors the shape of login()).
     */
    AuthenticationResponse register(RegisterRequest request);

    /**
     * Validates the supplied credentials against the stored (BCrypt-hashed)
     * password and returns an authentication response on success.
     */
    AuthenticationResponse login(AuthenticationRequest request);
}
