package com.mayureshpatel.pfdataservice.repository.user;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(UserRepository.class)
@DisplayName("UserRepository Integration Tests (PostgreSQL)")
class UserRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_USER = "testuser";
    private static final String TEST_EMAIL = "test@example.com";

    @Nested
    @DisplayName("Find Operations")
    class FindTests {
        @Test
        @DisplayName("should find all active users")
        void shouldFindAll() {
            // Act
            List<User> result = userRepository.findAll();

            // Assert
            assertFalse(result.isEmpty());
            assertTrue(result.size() >= 2);
        }

        @Test
        @DisplayName("should find by ID")
        void shouldFindById() {
            // Act
            Optional<User> result = userRepository.findById(1L);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(TEST_USER, result.get().getUsername());
        }

        @Test
        @DisplayName("should find by email")
        void shouldFindByEmail() {
            // Act
            Optional<User> result = userRepository.findByEmail(TEST_EMAIL);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(TEST_USER, result.get().getUsername());
        }

        @Test
        @DisplayName("should find by username")
        void shouldByUsername() {
            // Act
            Optional<User> result = userRepository.findByUsername(TEST_USER);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(TEST_EMAIL, result.get().getEmail());
        }
    }

    @Nested
    @DisplayName("Existence Checks")
    class ExistenceTests {
        @Test
        @DisplayName("should check existence by email")
        void shouldCheckEmail() {
            assertTrue(userRepository.existsByEmail(TEST_EMAIL));
            assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
        }

        @Test
        @DisplayName("should check existence by username")
        void shouldCheckUsername() {
            assertTrue(userRepository.existsByUsername(TEST_USER));
            assertFalse(userRepository.existsByUsername("unknown"));
        }

        @Test
        @DisplayName("should check existence by ID")
        void shouldCheckId() {
            assertTrue(userRepository.existsById(1L));
            assertFalse(userRepository.existsById(999L));
        }
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should insert a new user")
        void shouldInsert() {
            // Arrange
            User user = User.builder()
                    .username("newuser")
                    .email("new@example.com")
                    .passwordHash("hash")
                    .build();

            // Act
            int userId = userRepository.insert(user);

            // Assert
            assertTrue(userId > 0);
            assertTrue(userRepository.existsByUsername("newuser"));
        }

        @Test
        @DisplayName("should update an existing user")
        void shouldUpdate() {
            // Arrange
            User existing = userRepository.findById(1L).orElseThrow();
            User update = existing.toBuilder()
                    .email("updated@example.com")
                    .build();

            // Act
            int rows = userRepository.update(update);

            // Assert
            assertEquals(1, rows);
            User result = userRepository.findById(1L).orElseThrow();
            assertEquals("updated@example.com", result.getEmail());
        }

        @Test
        @DisplayName("should soft delete a user")
        void shouldDelete() {
            // Arrange
            User user = userRepository.findById(1L).orElseThrow();

            // Act
            int rows = userRepository.delete(user);

            // Assert
            assertEquals(1, rows);
            assertFalse(userRepository.existsById(1L));
        }

        @Test
        @DisplayName("should return 0 when deleting user with no ID")
        void shouldHandleNoIdDelete() {
            assertEquals(0, userRepository.delete(User.builder().build()));
        }
    }

    @Test
    @DisplayName("should count active users")
    void shouldCount() {
        assertEquals(2, userRepository.count());
    }
}
