package com.shopsphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Returned after a successful login or registration.
 * <p>
 * NOTE: "token" is a placeholder opaque value in Phase 2 (no real JWT/Security
 * has been wired up yet, per the Phase 2 scope). The Security phase will
 * replace the token-generation call inside AuthenticationServiceImpl with a
 * real signed JWT — this response shape does not need to change.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    private String token;
    private String tokenType;
    private UserResponse user;
}
