//package com.mayureshpatel.pfdataservice.repository.user;
//
//import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
//import com.mayureshpatel.pfdataservice.domain.user.User;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Transactional
//@DisplayName("UserRepository Integration Tests")
//class UserRepositoryTest extends BaseIntegrationTest {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Test
//    @DisplayName("save() should persist new user")
//    void save_shouldPersistNewUser() {
//        // Arrange
//        User user = new User();
//        user.setUsername("newuser");
//        user.setEmail("new@example.com");
//        user.setPasswordHash("hash123");
//
//        // Act
//        User saved = userRepository.save(user);
//
//        // Assert
//        assertThat(saved.getId()).isNotNull();
//        assertThat(saved.getUsername()).isEqualTo("newuser");
//
//        Optional<User> found = userRepository.findById(saved.getId());
//        assertThat(found).isPresent();
//        assertThat(found.get().getEmail()).isEqualTo("new@example.com");
//    }
//
//    @Test
//    @DisplayName("findByEmail() should return user if exists")
//    void findByEmail_shouldReturnUser() {
//        // Arrange
//        User user = new User();
//        user.setUsername("emailuser");
//        user.setEmail("findme@example.com");
//        user.setPasswordHash("hash");
//        userRepository.save(user);
//
//        // Act
//        Optional<User> found = userRepository.findByEmail("findme@example.com");
//
//        // Assert
//        assertThat(found).isPresent();
//        assertThat(found.get().getUsername()).isEqualTo("emailuser");
//    }
//
//    @Test
//    @DisplayName("findByUsername() should return user if exists")
//    void findByUsername_shouldReturnUser() {
//        // Arrange
//        User user = new User();
//        user.setUsername("uniqueuser");
//        user.setEmail("unique@example.com");
//        user.setPasswordHash("hash");
//        userRepository.save(user);
//
//        // Act
//        Optional<User> found = userRepository.findByUsername("uniqueuser");
//
//        // Assert
//        assertThat(found).isPresent();
//        assertThat(found.get().getEmail()).isEqualTo("unique@example.com");
//    }
//
//    @Test
//    @DisplayName("existsByEmail() should return true if exists")
//    void existsByEmail_shouldReturnTrue() {
//        // Arrange
//        User user = new User();
//        user.setUsername("checkemail");
//        user.setEmail("check@example.com");
//        user.setPasswordHash("hash");
//        userRepository.save(user);
//
//        // Act
//        boolean exists = userRepository.existsByEmail("check@example.com");
//
//        // Assert
//        assertThat(exists).isTrue();
//    }
//
//    @Test
//    @DisplayName("update() should update existing user")
//    void update_shouldUpdateUser() {
//        // Arrange
//        User user = new User();
//        user.setUsername("toupdate");
//        user.setEmail("update@example.com");
//        user.setPasswordHash("hash");
//        User saved = userRepository.save(user);
//
//        // Act
//        saved.setEmail("updated@example.com");
//        userRepository.save(saved);
//
//        // Assert
//        Optional<User> found = userRepository.findById(saved.getId());
//        assertThat(found).isPresent();
//        assertThat(found.get().getEmail()).isEqualTo("updated@example.com");
//    }
//
//    @Test
//    @DisplayName("delete() should remove user")
//    void delete_shouldRemoveUser() {
//        // Arrange
//        User user = new User();
//        user.setUsername("todelete");
//        user.setEmail("delete@example.com");
//        user.setPasswordHash("hash");
//        User saved = userRepository.save(user);
//
//        // Act
//        userRepository.delete(saved);
//
//        // Assert
//        Optional<User> found = userRepository.findById(saved.getId());
//        assertThat(found).isEmpty();
//    }
//
//    @Test
//    @DisplayName("existsByUsername() should return true when username exists")
//    void existsByUsername_shouldReturnTrue() {
//        // Arrange
//        User user = new User();
//        user.setUsername("existsuser");
//        user.setEmail("exists@example.com");
//        user.setPasswordHash("hash");
//        userRepository.save(user);
//
//        // Act & Assert
//        assertThat(userRepository.existsByUsername("existsuser")).isTrue();
//    }
//
//    @Test
//    @DisplayName("existsByUsername() should return false when username does not exist")
//    void existsByUsername_shouldReturnFalse() {
//        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
//    }
//
//    @Test
//    @DisplayName("existsByEmail() should return false when email does not exist")
//    void existsByEmail_shouldReturnFalse() {
//        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
//    }
//
//    @Test
//    @DisplayName("findByEmail() should return empty when email does not exist")
//    void findByEmail_shouldReturnEmpty() {
//        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
//    }
//
//    @Test
//    @DisplayName("findByUsername() should return empty when username does not exist")
//    void findByUsername_shouldReturnEmpty() {
//        assertThat(userRepository.findByUsername("nobody")).isEmpty();
//    }
//
//    @Test
//    @DisplayName("existsById() should return true when user exists")
//    void existsById_shouldReturnTrue() {
//        // Arrange
//        User user = new User();
//        user.setUsername("existsbyid");
//        user.setEmail("existsbyid@example.com");
//        user.setPasswordHash("hash");
//        User saved = userRepository.save(user);
//
//        // Act & Assert
//        assertThat(userRepository.existsById(saved.getId())).isTrue();
//    }
//
//    @Test
//    @DisplayName("existsById() should return false when user does not exist")
//    void existsById_shouldReturnFalse() {
//        assertThat(userRepository.existsById(99999L)).isFalse();
//    }
//}
