package com.shopsphere.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Persisted refresh token, one row per issued refresh token. Storing these
 * server-side (rather than trusting a long-lived stateless JWT alone) lets us
 * revoke individual sessions (logout) or all sessions for a user, and lets us
 * detect reuse of an already-rotated token.
 */
@Entity
@Table(name = "refresh_tokens", uniqueConstraints = {
        @UniqueConstraint(name = "uk_refresh_tokens_token", columnNames = "token")
}, indexes = {
        @Index(name = "idx_refresh_tokens_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // The raw refresh token string (a signed JWT itself, or an opaque random
    // value - JwtService currently issues a signed JWT for this).
    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_tokens_user"))
    private User user;

    /** True if the token is neither revoked nor past its expiry instant. */
    public boolean isValid() {
        return !Boolean.TRUE.equals(revoked) && expiryDate.isAfter(Instant.now());
    }
}
