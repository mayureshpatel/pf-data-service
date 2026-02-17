package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private UserRepository userRepository;

    private User buildUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$hashedpassword");
        return user;
    }

    @Test
    void insert_ShouldCreateUserWithGeneratedId() {
        // When
        User saved = userRepository.insert(buildUser("alice", "alice@example.com"));

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void save_NewUser_ShouldInsert() {
        // When
        User saved = userRepository.save(buildUser("bob", "bob@example.com"));

        // Then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void save_ExistingUser_ShouldUpdate() {
        // Given
        User saved = userRepository.insert(buildUser("charlie", "charlie@example.com"));
        saved.setUsername("charlie2");
        saved.setEmail("charlie2@example.com");
        saved.setLastUpdatedBy(saved.getId());

        // When
        User updated = userRepository.save(saved);

        // Then
        Optional<User> found = userRepository.findById(updated.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("charlie2");
        assertThat(found.get().getEmail()).isEqualTo("charlie2@example.com");
    }

    @Test
    void findById_ShouldReturnUser_WhenExists() {
        // Given
        User saved = userRepository.insert(buildUser("dave", "dave@example.com"));

        // When
        Optional<User> found = userRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("dave");
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        assertThat(userRepository.findById(999999L)).isEmpty();
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenExists() {
        // Given
        userRepository.insert(buildUser("eve", "eve@example.com"));

        // When
        Optional<User> found = userRepository.findByEmail("eve@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("eve");
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenNotExists() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        // Given
        userRepository.insert(buildUser("frank", "frank@example.com"));

        // When
        Optional<User> found = userRepository.findByUsername("frank");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("frank@example.com");
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenNotExists() {
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void findAll_ShouldReturnAllNonDeletedUsers() {
        // Given
        userRepository.insert(buildUser("grace", "grace@example.com"));

        // When
        List<User> users = userRepository.findAll();

        // Then
        assertThat(users).isNotEmpty();
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenExists() {
        userRepository.insert(buildUser("hank", "hank@example.com"));
        assertThat(userRepository.existsByEmail("hank@example.com")).isTrue();
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenNotExists() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenExists() {
        userRepository.insert(buildUser("ivan", "ivan@example.com"));
        assertThat(userRepository.existsByUsername("ivan")).isTrue();
    }

    @Test
    void existsByUsername_ShouldReturnFalse_WhenNotExists() {
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    @Test
    void existsById_ShouldReturnTrue_WhenExists() {
        User saved = userRepository.insert(buildUser("judy", "judy@example.com"));
        assertThat(userRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void existsById_ShouldReturnFalse_WhenNotExists() {
        assertThat(userRepository.existsById(999999L)).isFalse();
    }

    @Test
    void deleteById_ShouldSoftDelete() {
        // Given
        User saved = userRepository.insert(buildUser("karl", "karl@example.com"));

        // When
        userRepository.deleteById(saved.getId());

        // Then
        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void delete_ShouldSoftDelete() {
        // Given
        User saved = userRepository.insert(buildUser("lana", "lana@example.com"));

        // When
        userRepository.delete(saved);

        // Then
        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void count_ShouldReturnNumberOfActiveUsers() {
        // Given
        long before = userRepository.count();
        userRepository.insert(buildUser("mike", "mike@example.com"));

        // When / Then
        assertThat(userRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void update_ShouldPersistChanges() {
        // Given
        User saved = userRepository.insert(buildUser("nina", "nina@example.com"));
        saved.setUsername("nina2");
        saved.setEmail("nina2@example.com");
        saved.setLastUpdatedBy(saved.getId());

        // When
        userRepository.update(saved);

        // Then
        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("nina2");
    }
}
