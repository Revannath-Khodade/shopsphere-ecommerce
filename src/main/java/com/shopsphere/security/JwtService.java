package com.shopsphere.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain-facing JWT service: issues ShopSphere access and refresh tokens and
 * validates incoming ones. Delegates the actual signing/parsing mechanics to
 * {@link JwtTokenProvider}, keeping this class focused on "what claims go in
 * a ShopSphere token" rather than "how do you sign a JWT."
 * <p>
 * Access tokens carry the user's id and roles so the request filter can build
 * a full {@link org.springframework.security.core.Authentication} without an
 * extra database round-trip on every request. Refresh tokens intentionally
 * carry only the subject + a "type" claim - they're exchanged for a fresh
 * access token, never used to authorize a resource request directly.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenExpirationMs;

    public String generateAccessToken(CustomUserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userDetails.getId());
        claims.put(CLAIM_ROLES, extractAuthorityNames(userDetails));
        claims.put(CLAIM_TYPE, TOKEN_TYPE_ACCESS);

        Date expiration = new Date(System.currentTimeMillis() + accessTokenExpirationMs);
        return jwtTokenProvider.generateToken(userDetails.getUsername(), claims, expiration);
    }

    public String generateRefreshToken(CustomUserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userDetails.getId());
        claims.put(CLAIM_TYPE, TOKEN_TYPE_REFRESH);

        Date expiration = new Date(System.currentTimeMillis() + refreshTokenExpirationMs);
        return jwtTokenProvider.generateToken(userDetails.getUsername(), claims, expiration);
    }

    public String extractUsername(String token) {
        return jwtTokenProvider.extractUsername(token);
    }

    public Long extractUserId(String token) {
        return jwtTokenProvider.extractUserId(token);
    }

    public List<String> extractRoles(String token) {
        return jwtTokenProvider.extractRoles(token);
    }

    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(jwtTokenProvider.extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(jwtTokenProvider.extractTokenType(token));
    }

    public boolean isTokenExpired(String token) {
        return jwtTokenProvider.isTokenExpired(token);
    }

    /**
     * Full validation used by the request filter: signature must be intact,
     * the token must not be expired, its subject must match the loaded
     * UserDetails, and it must actually be an ACCESS token (a refresh token
     * presented as a bearer token on a normal API call is rejected).
     */
    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        if (!jwtTokenProvider.isTokenSignatureValid(token) || jwtTokenProvider.isTokenExpired(token)) {
            return false;
        }
        if (!isAccessToken(token)) {
            return false;
        }
        String username = extractUsername(token);
        return username != null && username.equals(userDetails.getUsername());
    }

    /**
     * Structural validation for a refresh token (signature + expiry + type).
     * Ownership / revocation is checked separately against the persisted
     * {@link com.shopsphere.entity.RefreshToken} row by RefreshTokenService,
     * since a structurally valid JWT could still have been revoked server-side.
     */
    public boolean isRefreshTokenStructurallyValid(String token) {
        return jwtTokenProvider.isTokenSignatureValid(token)
                && !jwtTokenProvider.isTokenExpired(token)
                && isRefreshToken(token);
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    private List<String> extractAuthorityNames(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }
}
