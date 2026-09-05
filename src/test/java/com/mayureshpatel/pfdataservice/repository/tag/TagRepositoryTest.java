package com.mayureshpatel.pfdataservice.repository.tag;

import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(TagRepository.class)
@DisplayName("TagRepository Integration Tests (PostgreSQL)")
class TagRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TagRepository repository;

    private static final Long USER_1 = 1L;
    private static final Long OTHER_USER = 999L;
    private static final Long BASELINE_TRANSACTION_ID = 1000L; // account_id=1 -> user_id=1

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {
        @Test
        @DisplayName("PF-307: should insert a new tag and return a real generated id "
                + "(previously returned the row-count from update(keyHolder) while never reading "
                + "the actual generated key back out of the holder)")
        void shouldInsertAndFind() {
            // Arrange
            Tag tag = Tag.builder()
                    .userId(USER_1)
                    .name("Test Tag")
                    .color("#000000")
                    .build();

            // Act
            Long generatedId = repository.insertAndReturnId(tag);

            // Assert
            assertNotNull(generatedId);
            assertTrue(generatedId > 0);
            Tag persisted = repository.findById(generatedId).orElseThrow();
            assertEquals("Test Tag", persisted.getName());
            assertEquals("#000000", persisted.getColor());
        }

        @Test
        @DisplayName("should find by ID")
        void shouldFindById() {
            // Arrange
            List<Tag> all = repository.findAllByUserId(USER_1); // Baseline has tags for USER_1
            Long id = all.get(0).getId();

            // Act
            Optional<Tag> result = repository.findById(id);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(id, result.get().getId());
        }

        @Test
        @DisplayName("PF-307: should find by ID and user ID")
        void shouldFindByIdAndUserId() {
            // Arrange
            List<Tag> all = repository.findAllByUserId(USER_1);
            Long id = all.get(0).getId();

            // Act
            Optional<Tag> matchingUser = repository.findById(id, USER_1);
            Optional<Tag> wrongUser = repository.findById(id, OTHER_USER);

            // Assert
            assertTrue(matchingUser.isPresent());
            assertTrue(wrongUser.isEmpty());
        }

        @Test
        @DisplayName("should update tag")
        void shouldUpdate() {
            // Arrange
            List<Tag> all = repository.findAllByUserId(USER_1);
            Tag existing = all.get(0);
            Tag update = existing.toBuilder().name("Updated Tag").color("#FFFFFF").build();

            // Act
            int rows = repository.update(update);

            // Assert
            assertEquals(1, rows);
            Tag result = repository.findById(existing.getId()).orElseThrow();
            assertEquals("Updated Tag", result.getName());
            assertEquals("#FFFFFF", result.getColor());
        }

        @Test
        @DisplayName("PF-307: update should affect zero rows when the userId doesn't match -- "
                + "previously unscoped (matched by id alone), so any caller who knew or guessed "
                + "another user's tag id could rename it")
        void shouldNotUpdateWhenUserIdDoesNotMatch() {
            // arrange
            List<Tag> all = repository.findAllByUserId(USER_1);
            Tag existing = all.get(0);
            String originalName = existing.getName();
            Tag updateAttempt = existing.toBuilder().name("SHOULD_NOT_APPLY").userId(OTHER_USER).build();

            // act
            int rows = repository.update(updateAttempt);

            // assert & verify
            assertEquals(0, rows);
            assertEquals(originalName, repository.findById(existing.getId()).orElseThrow().getName());
        }

        @Test
        @DisplayName("PF-307: should delete a tag by ID and UserID")
        void shouldDeleteById() {
            // Arrange
            Tag tag = Tag.builder().userId(USER_1).name("Delete Me").color("#000").build();
            Long id = repository.insertAndReturnId(tag);

            // Act
            int rows = repository.deleteById(id, USER_1);

            // Assert
            assertEquals(1, rows);
            assertTrue(repository.findById(id).isEmpty());
        }

        @Test
        @DisplayName("PF-307: delete should affect zero rows when the userId doesn't match -- "
                + "previously unscoped, the same gap update had")
        void shouldNotDeleteWhenUserIdDoesNotMatch() {
            // arrange
            Tag tag = Tag.builder().userId(USER_1).name("Should Survive").color("#000").build();
            Long id = repository.insertAndReturnId(tag);

            // act
            int rows = repository.deleteById(id, OTHER_USER);

            // assert & verify
            assertEquals(0, rows);
            assertTrue(repository.findById(id).isPresent());
        }

        @Test
        @DisplayName("should throw UnsupportedOperationException for insecure deleteById")
        void shouldThrowOnInsecureDelete() {
            assertThrows(UnsupportedOperationException.class, () -> repository.deleteById(1L));
        }

        @Test
        @DisplayName("should count tags")
        void shouldCount() {
            // Act
            long count = repository.count();

            // Assert
            assertTrue(count >= 2); // Baseline has 2 tags
        }

        @Test
        @DisplayName("PF-307: inserting a duplicate (user_id, name) violates the schema's own "
                + "unique constraint -- confirmed live against real Postgres, not assumed, since "
                + "GlobalExceptionHandler's DataIntegrityViolationException handler (400, not a "
                + "generic 500) only actually satisfies this ticket's AC if the constraint really "
                + "fires and Spring translates it to that exception type")
        void shouldViolateUniqueConstraintOnDuplicateName() {
            // arrange
            repository.insertAndReturnId(Tag.builder().userId(USER_1).name("Duplicate Name").color("#000").build());

            // act & assert
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                    repository.insertAndReturnId(Tag.builder().userId(USER_1).name("Duplicate Name").color("#111").build()));
        }
    }

    @Nested
    @DisplayName("Transaction Assignment (PF-307)")
    class TransactionAssignmentTests {
        @Test
        @DisplayName("should assign a tag to a transaction and find it by transaction ID")
        void shouldAssignAndFindByTransactionId() {
            // arrange
            Tag tag = Tag.builder().userId(USER_1).name("Assignable").color("#123456").build();
            Long tagId = repository.insertAndReturnId(tag);

            // act
            int rows = repository.insertTransactionTag(BASELINE_TRANSACTION_ID, tagId);
            List<Tag> tagsOnTransaction = repository.findByTransactionId(BASELINE_TRANSACTION_ID);

            // assert & verify
            assertEquals(1, rows);
            assertTrue(tagsOnTransaction.stream().anyMatch(t -> t.getId().equals(tagId)));
        }

        @Test
        @DisplayName("should remove a tag from a transaction without affecting other tags on it")
        void shouldRemoveFromTransaction() {
            // arrange
            Tag tagA = Tag.builder().userId(USER_1).name("KeepMe").color("#111").build();
            Tag tagB = Tag.builder().userId(USER_1).name("RemoveMe").color("#222").build();
            Long tagAId = repository.insertAndReturnId(tagA);
            Long tagBId = repository.insertAndReturnId(tagB);
            repository.insertTransactionTag(BASELINE_TRANSACTION_ID, tagAId);
            repository.insertTransactionTag(BASELINE_TRANSACTION_ID, tagBId);

            // act
            int rows = repository.deleteTransactionTag(BASELINE_TRANSACTION_ID, tagBId);
            List<Tag> remaining = repository.findByTransactionId(BASELINE_TRANSACTION_ID);

            // assert & verify
            assertEquals(1, rows);
            assertTrue(remaining.stream().anyMatch(t -> t.getId().equals(tagAId)));
            assertTrue(remaining.stream().noneMatch(t -> t.getId().equals(tagBId)));
        }

        @Test
        @DisplayName("PF-307: deleting a tag also removes its transaction_tags rows "
                + "(ON DELETE CASCADE), confirmed live rather than assumed from the schema")
        void shouldCascadeDeleteTransactionAssignments() {
            // arrange
            Tag tag = Tag.builder().userId(USER_1).name("Cascade Me").color("#333").build();
            Long tagId = repository.insertAndReturnId(tag);
            repository.insertTransactionTag(BASELINE_TRANSACTION_ID, tagId);
            assertTrue(repository.findByTransactionId(BASELINE_TRANSACTION_ID).stream()
                    .anyMatch(t -> t.getId().equals(tagId)), "sanity check: assignment exists before delete");

            // act
            repository.deleteById(tagId, USER_1);

            // assert & verify
            assertTrue(repository.findByTransactionId(BASELINE_TRANSACTION_ID).stream()
                    .noneMatch(t -> t.getId().equals(tagId)));
        }
    }
}
