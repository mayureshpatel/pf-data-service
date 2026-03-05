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

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {
        @Test
        @DisplayName("should insert and find tag")
        void shouldInsertAndFind() {
            // Arrange
            Tag tag = Tag.builder()
                    .userId(USER_1)
                    .name("Test Tag")
                    .color("#000000")
                    .build();

            // Act
            int rows = repository.insert(tag);
            List<Tag> all = repository.findAllByUserId(USER_1);

            // Assert
            assertEquals(1, rows);
            assertTrue(all.stream().anyMatch(t -> t.getName().equals("Test Tag")));
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
        @DisplayName("should delete by ID")
        void shouldDeleteById() {
            // Arrange
            Tag tag = Tag.builder().userId(USER_1).name("Delete Me").color("#000").build();
            repository.insert(tag);
            Long id = repository.findAllByUserId(USER_1).stream()
                    .filter(t -> t.getName().equals("Delete Me"))
                    .findFirst().orElseThrow().getId();

            // Act
            int rows = repository.deleteById(id);

            // Assert
            assertEquals(1, rows);
            assertTrue(repository.findById(id).isEmpty());
        }

        @Test
        @DisplayName("should count tags")
        void shouldCount() {
            // Act
            long count = repository.count();

            // Assert
            assertTrue(count >= 2); // Baseline has 2 tags
        }
    }
}
