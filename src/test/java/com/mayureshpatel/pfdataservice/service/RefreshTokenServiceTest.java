package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.config.RefreshTokenProperties;
import com.mayureshpatel.pfdataservice.domain.user.RefreshToken;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.user.RefreshTokenRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies {@link RefreshTokenService} issue, renew, revoke and purge rules against a fixed clock and mocked repositories. */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Tests")
class RefreshTokenServiceTest {

    private static final Instant NOW_INSTANT = Instant.parse("2026-11-05T12:00:00Z");
    private static final OffsetDateTime NOW = OffsetDateTime.ofInstant(NOW_INSTANT, ZoneOffset.UTC);
    private static final String RAW = "raw-token";
    private static final long TOKEN_ID = 7L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;

    private RefreshTokenService service;
    private final User user = User.builder().id(1L).username("u").build();

    @BeforeEach
    void setUp() {
        RefreshTokenProperties properties = new RefreshTokenProperties(
                Duration.ofDays(14), Duration.ofDays(60), Duration.ofHours(8), Duration.ofHours(24),
                "pf_refresh", true, "Strict");
        service = new RefreshTokenService(refreshTokenRepository, userRepository, properties,
                Clock.fixed(NOW_INSTANT, ZoneOffset.UTC));
    }

    private RefreshToken stored(boolean rememberMe, OffsetDateTime idle, OffsetDateTime absolute, OffsetDateTime revoked) {
        return RefreshToken.builder().id(TOKEN_ID).userId(1L).tokenHash(RefreshTokenService.hash(RAW))
                .rememberMe(rememberMe).idleExpiresAt(idle).absoluteExpiresAt(absolute).revokedAt(revoked).build();
    }

    /** Issuing stores only a hash and the right expiries per mode. */
    @Nested
    @DisplayName("issue")
    class IssueTests {

        @Test
        @DisplayName("should store only the hash and use remember-me windows with a cookie max-age")
        void shouldIssueRememberMe() {
            // arrange
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

            // act
            IssuedRefreshToken issued = service.issue(user, true);

            // assert & verify
            verify(refreshTokenRepository).insert(captor.capture(), eq(NOW));
            RefreshToken saved = captor.getValue();
            assertThat(saved.getTokenHash()).isEqualTo(RefreshTokenService.hash(issued.rawToken())).isNotEqualTo(issued.rawToken());
            assertThat(saved.getTokenHash()).hasSize(64);
            assertThat(issued.rawToken()).matches("[A-Za-z0-9_-]{43}");
            assertThat(saved.isRememberMe()).isTrue();
            assertThat(saved.getUserId()).isEqualTo(1L);
            assertThat(saved.getIdleExpiresAt()).isEqualTo(NOW.plusDays(14));
            assertThat(saved.getAbsoluteExpiresAt()).isEqualTo(NOW.plusDays(60));
            assertThat(issued.rememberMe()).isTrue();
            assertThat(issued.maxAgeSeconds()).isEqualTo(Duration.ofDays(14).toSeconds());
        }

        @Test
        @DisplayName("should use session-only windows and no cookie max-age")
        void shouldIssueSessionOnly() {
            // arrange
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

            // act
            IssuedRefreshToken issued = service.issue(user, false);

            // assert & verify
            verify(refreshTokenRepository).insert(captor.capture(), eq(NOW));
            assertThat(captor.getValue().isRememberMe()).isFalse();
            assertThat(captor.getValue().getIdleExpiresAt()).isEqualTo(NOW.plusHours(8));
            assertThat(captor.getValue().getAbsoluteExpiresAt()).isEqualTo(NOW.plusHours(24));
            assertThat(issued.maxAgeSeconds()).isNull();
            assertThat(issued.toString()).doesNotContain(issued.rawToken());
        }

        @Test
        @DisplayName("should generate a different token each time")
        void shouldGenerateUniqueTokens() {
            // act
            String first = service.issue(user, false).rawToken();
            String second = service.issue(user, false).rawToken();

            // assert & verify
            assertThat(first).isNotEqualTo(second);
        }
    }

    /** Renewing accepts only live tokens of existing users and slides idle without rotating. */
    @Nested
    @DisplayName("renew")
    class RenewTests {

        @Test
        @DisplayName("should slide the idle expiry and return the user")
        void shouldRenew() {
            // arrange
            when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(RAW)))
                    .thenReturn(Optional.of(stored(false, NOW.plusHours(1), NOW.plusHours(20), null)));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // act
            Optional<RenewedRefreshToken> result = service.renew(RAW);

            // assert & verify
            assertThat(result).isPresent();
            assertThat(result.get().user()).isSameAs(user);
            assertThat(result.get().rememberMe()).isFalse();
            assertThat(result.get().maxAgeSeconds()).isNull();
            verify(refreshTokenRepository).touch(TOKEN_ID, NOW, NOW.plusHours(8));
        }

        @Test
        @DisplayName("should never slide idle beyond the absolute expiry")
        void shouldCapAtAbsolute() {
            // arrange
            OffsetDateTime absolute = NOW.plusHours(2);
            when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(RAW)))
                    .thenReturn(Optional.of(stored(true, NOW.plusHours(1), absolute, null)));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // act
            service.renew(RAW);

            // assert & verify
            verify(refreshTokenRepository).touch(TOKEN_ID, NOW, absolute);
        }

        @Test
        @DisplayName("should reject an unknown, null or blank token")
        void shouldRejectUnknown() {
            // arrange
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

            // act & assert & verify
            assertThat(service.renew(RAW)).isEmpty();
            assertThat(service.renew(null)).isEmpty();
            assertThat(service.renew(" ")).isEmpty();
            verify(refreshTokenRepository, never()).touch(anyLong(), any(), any());
        }

        @Test
        @DisplayName("should reject a token past its idle expiry")
        void shouldRejectIdleExpired() {
            // arrange
            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.of(stored(true, NOW, NOW.plusDays(5), null)));

            // act & assert & verify
            assertThat(service.renew(RAW)).isEmpty();
            verify(refreshTokenRepository, never()).touch(anyLong(), any(), any());
        }

        @Test
        @DisplayName("should reject a token past its absolute expiry")
        void shouldRejectAbsoluteExpired() {
            // arrange
            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.of(stored(true, NOW.plusDays(1), NOW.minusSeconds(1), null)));

            // act & assert & verify
            assertThat(service.renew(RAW)).isEmpty();
            verify(refreshTokenRepository, never()).touch(anyLong(), any(), any());
        }

        @Test
        @DisplayName("should reject a revoked token")
        void shouldRejectRevoked() {
            // arrange
            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.of(stored(true, NOW.plusDays(1), NOW.plusDays(2), NOW.minusHours(1))));

            // act & assert & verify
            assertThat(service.renew(RAW)).isEmpty();
            verify(refreshTokenRepository, never()).touch(anyLong(), any(), any());
        }

        @Test
        @DisplayName("should reject a token whose user no longer exists")
        void shouldRejectDeletedUser() {
            // arrange
            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.of(stored(true, NOW.plusDays(1), NOW.plusDays(2), null)));
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThat(service.renew(RAW)).isEmpty();
            verify(refreshTokenRepository, never()).touch(anyLong(), any(), any());
        }
    }

    /** Revoking and purging delegate with the clock's time. */
    @Nested
    @DisplayName("revoke and purge")
    class RevokeAndPurgeTests {

        @Test
        @DisplayName("should revoke by hash and be a no-op for a blank token")
        void shouldRevoke() {
            // act
            service.revoke(RAW);
            service.revoke(RAW);
            service.revoke(null);
            service.revoke("");

            // assert & verify
            verify(refreshTokenRepository, org.mockito.Mockito.times(2)).revokeByTokenHash(RefreshTokenService.hash(RAW), NOW);
            verify(refreshTokenRepository, never()).revokeByTokenHash(eq(RefreshTokenService.hash("")), any());
        }

        @Test
        @DisplayName("should purge rows expired or revoked more than a day ago")
        void shouldPurge() {
            // arrange
            when(refreshTokenRepository.deleteExpiredOrRevokedBefore(NOW.minusDays(1))).thenReturn(3);

            // act
            int deleted = service.purgeExpired();

            // assert & verify
            assertThat(deleted).isEqualTo(3);
            verify(refreshTokenRepository).deleteExpiredOrRevokedBefore(NOW.minusDays(1));
        }
    }
}
