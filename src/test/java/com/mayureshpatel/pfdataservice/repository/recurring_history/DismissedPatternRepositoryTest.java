package com.mayureshpatel.pfdataservice.repository.recurring_history;

import com.mayureshpatel.pfdataservice.dto.transaction.recurring.DismissedPatternDto;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code DismissedPatternRepository} against a real PostgreSQL instance: user-scoped listing, an idempotent insert keyed on (user, pattern key), and owner-only deletion. */
@Import(DismissedPatternRepository.class)
@DisplayName("DismissedPatternRepository Integration Tests (PostgreSQL)")
class DismissedPatternRepositoryTest extends BaseRepositoryTest {

    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;
    private static final String NETFLIX_KEY = "netflix.com 866";
    private static final String NETFLIX_NAME = "NETFLIX.COM 866";

    @Autowired
    private DismissedPatternRepository repository;

    /** Dismissals are created once per (user, key), listed newest first for their owner only, and exposed as a key set for the detection exclusion. */
    @Nested
    @DisplayName("insert and list")
    class InsertAndListTests {

        @Test
        @DisplayName("should insert a dismissal and list only the user's own")
        void shouldListOnlyOwn() {
            // arrange
            repository.insert(USER_1, NETFLIX_KEY, NETFLIX_NAME);
            repository.insert(USER_2, "gym", "Gym");

            // act
            List<DismissedPatternDto> mine = repository.findByUserId(USER_1);
            Set<String> keys = repository.findKeysByUserId(USER_1);

            // assert & verify
            assertEquals(1, mine.size());
            assertEquals(NETFLIX_KEY, mine.get(0).patternKey());
            assertEquals(NETFLIX_NAME, mine.get(0).name());
            assertNotNull(mine.get(0).dismissedAt());
            assertEquals(Set.of(NETFLIX_KEY), keys);
        }

        @Test
        @DisplayName("should ignore a second dismissal of the same key and return the same id")
        void shouldBeIdempotent() {
            // arrange
            Long first = repository.insert(USER_1, NETFLIX_KEY, NETFLIX_NAME);

            // act
            Long second = repository.insert(USER_1, NETFLIX_KEY, "Netflix");

            // assert & verify
            assertEquals(first, second);
            assertEquals(1, repository.findByUserId(USER_1).size());
        }

        @Test
        @DisplayName("should allow two users to dismiss the same key")
        void shouldAllowSameKeyForDifferentUsers() {
            // act
            Long a = repository.insert(USER_1, NETFLIX_KEY, NETFLIX_NAME);
            Long b = repository.insert(USER_2, NETFLIX_KEY, NETFLIX_NAME);

            // assert & verify
            assertNotEquals(a, b);
        }
    }

    /** A dismissal can only be removed by the user who made it. */
    @Nested
    @DisplayName("deleteByIdAndUserId")
    class DeleteTests {

        @Test
        @DisplayName("should delete only the owner's dismissal")
        void shouldDeleteOnlyOwners() {
            // arrange
            Long id = repository.insert(USER_1, NETFLIX_KEY, NETFLIX_NAME);

            // act
            int byOther = repository.deleteByIdAndUserId(id, USER_2);
            int byOwner = repository.deleteByIdAndUserId(id, USER_1);

            // assert & verify
            assertEquals(0, byOther);
            assertEquals(1, byOwner);
            assertTrue(repository.findByUserId(USER_1).isEmpty());
        }
    }
}
