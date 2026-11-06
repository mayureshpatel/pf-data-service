package com.mayureshpatel.pfdataservice.repository.user;

import com.mayureshpatel.pfdataservice.domain.user.RefreshToken;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies {@code RefreshTokenRepository} against a real PostgreSQL instance (Testcontainers), including the unique hash and the cascade with the owning user. */
@Import(RefreshTokenRepository.class)
@DisplayName("RefreshTokenRepository Integration Tests (PostgreSQL)")
class RefreshTokenRepositoryTest extends BaseRepositoryTest {

    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;
    private static final String HASH_R = "hash-r";
    private static final String P_ID = "id";
    private static final String P_T = "t";
    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 5, 12, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private RefreshTokenRepository repository;
    @Autowired
    private JdbcClient jdbcClient;

    private long insert(Long userId, String hash) {
        return repository.insert(RefreshToken.builder().userId(userId).tokenHash(hash).rememberMe(true)
                .idleExpiresAt(NOW.plusDays(14)).absoluteExpiresAt(NOW.plusDays(60)).build(), NOW);
    }

    /** Insert returns the id and find reads the row back. */
    @Nested
    @DisplayName("insert and findByTokenHash")
    class InsertAndFindTests {

        @Test
        @DisplayName("should insert a token and find it by hash with all fields")
        void shouldInsertAndFind() {
            // act
            long id = insert(USER_1, "hash-a");
            RefreshToken found = repository.findByTokenHash("hash-a").orElseThrow();

            // assert & verify
            assertThat(found.getId()).isEqualTo(id);
            assertThat(found.getUserId()).isEqualTo(USER_1);
            assertThat(found.isRememberMe()).isTrue();
            assertThat(found.getCreatedAt().toInstant()).isEqualTo(NOW.toInstant());
            assertThat(found.getLastUsedAt().toInstant()).isEqualTo(NOW.toInstant());
            assertThat(found.getIdleExpiresAt().toInstant()).isEqualTo(NOW.plusDays(14).toInstant());
            assertThat(found.getAbsoluteExpiresAt().toInstant()).isEqualTo(NOW.plusDays(60).toInstant());
            assertThat(found.getRevokedAt()).isNull();
        }

        @Test
        @DisplayName("should return empty for an unknown hash")
        void shouldReturnEmpty() {
            // act & assert & verify
            assertThat(repository.findByTokenHash("nope")).isEmpty();
        }

        @Test
        @DisplayName("should reject a duplicate token hash")
        void shouldRejectDuplicateHash() {
            // arrange
            insert(USER_1, "dup");

            // act & assert & verify
            assertThatThrownBy(() -> insert(USER_2, "dup")).isInstanceOf(DuplicateKeyException.class);
        }
    }

    /** Touch slides the idle expiry and last use. */
    @Nested
    @DisplayName("touch")
    class TouchTests {

        @Test
        @DisplayName("should update last used and idle expiry")
        void shouldTouch() {
            // arrange
            long id = insert(USER_1, "hash-t");

            // act
            int updated = repository.touch(id, NOW.plusHours(3), NOW.plusDays(15));

            // assert & verify
            RefreshToken found = repository.findByTokenHash("hash-t").orElseThrow();
            assertThat(updated).isEqualTo(1);
            assertThat(found.getLastUsedAt().toInstant()).isEqualTo(NOW.plusHours(3).toInstant());
            assertThat(found.getIdleExpiresAt().toInstant()).isEqualTo(NOW.plusDays(15).toInstant());
            assertThat(found.getAbsoluteExpiresAt().toInstant()).isEqualTo(NOW.plusDays(60).toInstant());
        }
    }

    /** Revoking is idempotent and scoped. */
    @Nested
    @DisplayName("revoke")
    class RevokeTests {

        @Test
        @DisplayName("should revoke once and keep the first revoke time")
        void shouldRevokeIdempotently() {
            // arrange
            insert(USER_1, HASH_R);

            // act
            int first = repository.revokeByTokenHash(HASH_R, NOW.plusHours(1));
            int second = repository.revokeByTokenHash(HASH_R, NOW.plusHours(2));

            // assert & verify
            assertThat(first).isEqualTo(1);
            assertThat(second).isZero();
            assertThat(repository.findByTokenHash(HASH_R).orElseThrow().getRevokedAt().toInstant())
                    .isEqualTo(NOW.plusHours(1).toInstant());
        }

        @Test
        @DisplayName("should revoke every live token of one user only")
        void shouldRevokeAllForUser() {
            // arrange
            insert(USER_1, "u1-a");
            insert(USER_1, "u1-b");
            insert(USER_2, "u2-a");

            // act
            int revoked = repository.revokeAllByUserId(USER_1, NOW.plusHours(1));

            // assert & verify
            assertThat(revoked).isEqualTo(2);
            assertThat(repository.findByTokenHash("u1-a").orElseThrow().getRevokedAt()).isNotNull();
            assertThat(repository.findByTokenHash("u1-b").orElseThrow().getRevokedAt()).isNotNull();
            assertThat(repository.findByTokenHash("u2-a").orElseThrow().getRevokedAt()).isNull();
        }
    }

    /** Purge removes only rows that expired or were revoked before the cutoff. */
    @Nested
    @DisplayName("deleteExpiredOrRevokedBefore")
    class PurgeTests {

        @Test
        @DisplayName("should delete expired and revoked rows older than the cutoff and keep the rest")
        void shouldPurge() {
            // arrange
            insert(USER_1, "live");
            insert(USER_1, "revoked-old");
            insert(USER_1, "revoked-new");
            long idleOld = insert(USER_1, "idle-old");
            long absoluteOld = insert(USER_1, "absolute-old");
            repository.revokeByTokenHash("revoked-old", NOW.minusDays(3));
            repository.revokeByTokenHash("revoked-new", NOW.plusHours(1));
            jdbcClient.sql("update refresh_tokens set idle_expires_at = :t where id = :id")
                    .param(P_T, NOW.minusDays(2)).param(P_ID, idleOld).update();
            jdbcClient.sql("update refresh_tokens set absolute_expires_at = :t where id = :id")
                    .param(P_T, NOW.minusDays(2)).param(P_ID, absoluteOld).update();

            // act
            int deleted = repository.deleteExpiredOrRevokedBefore(NOW.minusDays(1));

            // assert & verify
            assertThat(deleted).isEqualTo(3);
            assertThat(repository.findByTokenHash("live")).isPresent();
            assertThat(repository.findByTokenHash("revoked-new")).isPresent();
            assertThat(repository.findByTokenHash("revoked-old")).isEmpty();
            assertThat(repository.findByTokenHash("idle-old")).isEmpty();
            assertThat(repository.findByTokenHash("absolute-old")).isEmpty();
        }
    }

    /** Deleting the owning user deletes the user's tokens. */
    @Nested
    @DisplayName("user cascade")
    class CascadeTests {

        @Test
        @DisplayName("should delete tokens when the owning user is deleted")
        void shouldCascade() {
            // arrange
            insert(USER_2, "cascade");
            jdbcClient.sql("delete from merchants where user_id = :id").param(P_ID, USER_2).update();

            // act
            jdbcClient.sql("delete from users where id = :id").param(P_ID, USER_2).update();

            // assert & verify
            assertThat(repository.findByTokenHash("cascade")).isEmpty();
        }
    }
}
