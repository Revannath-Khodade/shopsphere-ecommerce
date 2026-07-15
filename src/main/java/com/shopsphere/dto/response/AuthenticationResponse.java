package com.shopsphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Returned after a successful login, registration, or token refresh.
 * "expiresIn" is the access token's lifetime in seconds, so clients can
 * proactively refresh shortly before expiry instead of waiting for a 401.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    private String token;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserResponse user;
}
