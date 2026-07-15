package com.shopsphere.service;

import com.shopsphere.entity.RefreshToken;
import com.shopsphere.entity.User;

public interface RefreshTokenService {

    /** Issues and persists a new refresh token row for the given user. */
    RefreshToken createRefreshToken(User user, String tokenValue, long expirationMs);

    /**
     * Looks up a refresh token by its raw string value and verifies it is
     * still valid (not revoked, not expired). Throws if the token is unknown
     * or invalid, so callers never need a separate existence check.
     */
    RefreshToken verifyAndGet(String tokenValue);

    /** Revokes a single refresh token (used on logout from one device). */
    void revokeToken(String tokenValue);

    /** Revokes every active refresh token belonging to a user (logout from all devices, password change, etc.). */
    void revokeAllForUser(Long userId);
}
