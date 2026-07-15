package com.shopsphere.service.impl;

import com.shopsphere.entity.RefreshToken;
import com.shopsphere.entity.User;
import com.shopsphere.exception.UnauthorizedException;
import com.shopsphere.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(10L).username("amit_verma").build();
    }

    @Test
    void createRefreshToken_shouldPersistTokenWithFutureExpiry() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user, "raw-token-value", 604_800_000L);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getToken()).isEqualTo("raw-token-value");
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getRevoked()).isFalse();
        assertThat(captor.getValue().getExpiryDate()).isAfter(Instant.now());
        assertThat(result).isNotNull();
    }

    @Test
    void verifyAndGet_shouldReturnToken_whenValidAndNotExpiredOrRevoked() {
        RefreshToken validToken = RefreshToken.builder()
                .id(1L)
                .token("valid-token")
                .user(user)
                .revoked(false)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(validToken));

        RefreshToken result = refreshTokenService.verifyAndGet("valid-token");

        assertThat(result).isEqualTo(validToken);
    }

    @Test
    void verifyAndGet_shouldThrowUnauthorizedException_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyAndGet("missing-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verifyAndGet_shouldThrowUnauthorizedException_whenTokenIsRevoked() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(1L)
                .token("revoked-token")
                .user(user)
                .revoked(true)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.verifyAndGet("revoked-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verifyAndGet_shouldThrowUnauthorizedException_whenTokenIsExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token("expired-token")
                .user(user)
                .revoked(false)
                .expiryDate(Instant.now().minusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.verifyAndGet("expired-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void revokeToken_shouldMarkTokenAsRevoked_whenTokenExists() {
        RefreshToken token = RefreshToken.builder().id(1L).token("some-token").user(user).revoked(false).build();
        when(refreshTokenRepository.findByToken("some-token")).thenReturn(Optional.of(token));

        refreshTokenService.revokeToken("some-token");

        assertThat(token.getRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revokeToken_shouldDoNothing_whenTokenDoesNotExist() {
        when(refreshTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        refreshTokenService.revokeToken("missing-token");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeAllForUser_shouldDelegateToRepositoryBulkUpdate() {
        refreshTokenService.revokeAllForUser(10L);

        verify(refreshTokenRepository).revokeAllByUserId(10L);
    }
}
