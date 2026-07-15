package com.shopsphere.service.impl;

import com.shopsphere.entity.RefreshToken;
import com.shopsphere.entity.User;
import com.shopsphere.exception.UnauthorizedException;
import com.shopsphere.repository.RefreshTokenRepository;
import com.shopsphere.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user, String tokenValue, long expirationMs) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiryDate(Instant.now().plusMillis(expirationMs))
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token issued: id={}, userId={}, expiresAt={}", saved.getId(), user.getId(), saved.getExpiryDate());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verifyAndGet(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found (possibly forged or already deleted)");
                    return new UnauthorizedException("Invalid refresh token.");
                });

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            log.warn("Attempted reuse of a revoked refresh token: id={}, userId={}",
                    refreshToken.getId(), refreshToken.getUser().getId());
            throw new UnauthorizedException("This refresh token has been revoked. Please log in again.");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            log.info("Refresh token expired: id={}, userId={}", refreshToken.getId(), refreshToken.getUser().getId());
            throw new UnauthorizedException("Your session has expired. Please log in again.");
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeToken(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Refresh token revoked: id={}, userId={}", token.getId(), token.getUser().getId());
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("All refresh tokens revoked for userId={}", userId);
    }
}
