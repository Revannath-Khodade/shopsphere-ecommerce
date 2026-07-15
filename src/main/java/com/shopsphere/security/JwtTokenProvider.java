package com.shopsphere.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Low-level JWT primitives: signing, parsing, and claim extraction. Contains
 * no ShopSphere-specific domain knowledge (no User/Role types) - that
 * orchestration lives one layer up, in {@link JwtService}. Keeping this
 * separation means the signing/parsing mechanics can be tested (and
 * reasoned about) in complete isolation from the authentication flow.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret) {
        // The configured secret is treated as raw key material for HMAC-SHA.
        // It must be long enough (>= 256 bits once UTF-8 encoded) for the
        // HS256 algorithm jjwt selects automatically for a SecretKey of this size.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Builds and signs a compact JWT string.
     *
     * @param subject        typically the username
     * @param extraClaims    additional claims to embed (e.g. "roles", "userId", "type")
     * @param expirationDate absolute expiry instant for this token
     */
    public String generateToken(String subject, Map<String, Object> extraClaims, Date expirationDate) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(expirationDate)
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        if (roles instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    public Long extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public String extractTokenType(String token) {
        Object type = extractAllClaims(token).get("type");
        return type == null ? null : type.toString();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Verifies the token's signature and structural validity (NOT expiration -
     * callers combine this with {@link #isTokenExpired(String)} as needed).
     * Returns false rather than throwing, so callers can fail closed with a
     * simple boolean check instead of a try/catch at every call site.
     */
    public boolean isTokenSignatureValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // Signature is still valid even though the token has expired -
            // the caller may want to distinguish "expired" from "tampered".
            return true;
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT parsing failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
