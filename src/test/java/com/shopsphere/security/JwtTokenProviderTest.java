package com.shopsphere.security;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    // 256-bit+ secret suitable for HS256 - test-only value, never used in production.
    private static final String TEST_SECRET = "test-secret-key-for-jwt-signing-must-be-at-least-256-bits-long!!";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET);
    }

    @Test
    void generateToken_shouldEmbedSubjectAndClaims_thatCanBeExtractedBack() {
        Map<String, Object> claims = Map.of("userId", 42L, "roles", List.of("ROLE_CUSTOMER"), "type", "ACCESS");
        Date expiration = new Date(System.currentTimeMillis() + 60_000);

        String token = jwtTokenProvider.generateToken("amit_verma", claims, expiration);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("amit_verma");
        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtTokenProvider.extractRoles(token)).containsExactly("ROLE_CUSTOMER");
        assertThat(jwtTokenProvider.extractTokenType(token)).isEqualTo("ACCESS");
    }

    @Test
    void isTokenExpired_shouldReturnFalse_forFreshlyIssuedToken() {
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        String token = jwtTokenProvider.generateToken("amit_verma", Map.of(), expiration);

        assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenExpired_shouldReturnTrue_forAlreadyExpiredToken() {
        Date alreadyExpired = new Date(System.currentTimeMillis() - 1_000);
        String token = jwtTokenProvider.generateToken("amit_verma", Map.of(), alreadyExpired);

        assertThat(jwtTokenProvider.isTokenExpired(token)).isTrue();
    }

    @Test
    void isTokenSignatureValid_shouldReturnTrue_forTokenSignedWithSameKey() {
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        String token = jwtTokenProvider.generateToken("amit_verma", Map.of(), expiration);

        assertThat(jwtTokenProvider.isTokenSignatureValid(token)).isTrue();
    }

    @Test
    void isTokenSignatureValid_shouldReturnFalse_forTokenSignedWithDifferentKey() {
        JwtTokenProvider otherProvider = new JwtTokenProvider("a-completely-different-secret-key-also-256-bits-long-enough!!!");
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        String tokenFromOtherKey = otherProvider.generateToken("amit_verma", Map.of(), expiration);

        assertThat(jwtTokenProvider.isTokenSignatureValid(tokenFromOtherKey)).isFalse();
    }

    @Test
    void extractAllClaims_shouldThrowSignatureException_whenTokenIsTampered() {
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        String token = jwtTokenProvider.generateToken("amit_verma", Map.of(), expiration);
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtTokenProvider.extractUsername(tamperedToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void extractRoles_shouldReturnEmptyList_whenNoRolesClaimPresent() {
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        String token = jwtTokenProvider.generateToken("amit_verma", Map.of(), expiration);

        assertThat(jwtTokenProvider.extractRoles(token)).isEmpty();
    }
}
