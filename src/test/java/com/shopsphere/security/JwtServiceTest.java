package com.shopsphere.security;

import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-for-jwt-signing-must-be-at-least-256-bits-long!!";
    private static final long ACCESS_EXPIRATION_MS = 900_000L;   // 15 minutes
    private static final long REFRESH_EXPIRATION_MS = 604_800_000L; // 7 days

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(TEST_SECRET);
        jwtService = new JwtService(jwtTokenProvider);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", ACCESS_EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", REFRESH_EXPIRATION_MS);

        Set<Role> roles = new HashSet<>();
        roles.add(Role.builder().id(3L).name(RoleName.ROLE_CUSTOMER).build());

        User user = User.builder()
                .id(10L)
                .username("amit_verma")
                .email("amit@example.com")
                .password("hashed-password")
                .enabled(true)
                .accountNonLocked(true)
                .roles(roles)
                .build();

        userDetails = new CustomUserDetails(user);
    }

    @Test
    void generateAccessToken_shouldBeValidAgainstMatchingUserDetails() {
        String accessToken = jwtService.generateAccessToken(userDetails);

        assertThat(jwtService.isAccessTokenValid(accessToken, userDetails)).isTrue();
        assertThat(jwtService.extractUsername(accessToken)).isEqualTo("amit_verma");
        assertThat(jwtService.extractUserId(accessToken)).isEqualTo(10L);
        assertThat(jwtService.extractRoles(accessToken)).contains("ROLE_CUSTOMER");
        assertThat(jwtService.isAccessToken(accessToken)).isTrue();
        assertThat(jwtService.isRefreshToken(accessToken)).isFalse();
    }

    @Test
    void generateRefreshToken_shouldBeStructurallyValidAndTypedAsRefresh() {
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        assertThat(jwtService.isRefreshTokenStructurallyValid(refreshToken)).isTrue();
        assertThat(jwtService.isRefreshToken(refreshToken)).isTrue();
        assertThat(jwtService.isAccessToken(refreshToken)).isFalse();
    }

    @Test
    void isAccessTokenValid_shouldReturnFalse_whenUsernameDoesNotMatch() {
        String accessToken = jwtService.generateAccessToken(userDetails);

        User anotherUser = User.builder()
                .id(99L)
                .username("someone_else")
                .password("x")
                .enabled(true)
                .accountNonLocked(true)
                .roles(new HashSet<>())
                .build();
        UserDetails otherUserDetails = new CustomUserDetails(anotherUser);

        assertThat(jwtService.isAccessTokenValid(accessToken, otherUserDetails)).isFalse();
    }

    @Test
    void isAccessTokenValid_shouldReturnFalse_whenARefreshTokenIsPresentedAsAnAccessToken() {
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        assertThat(jwtService.isAccessTokenValid(refreshToken, userDetails)).isFalse();
    }

    @Test
    void getAccessTokenExpirationMs_and_getRefreshTokenExpirationMs_shouldReturnConfiguredValues() {
        assertThat(jwtService.getAccessTokenExpirationMs()).isEqualTo(ACCESS_EXPIRATION_MS);
        assertThat(jwtService.getRefreshTokenExpirationMs()).isEqualTo(REFRESH_EXPIRATION_MS);
    }
}
