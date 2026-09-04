package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static final String USERNAME = "testuser";
    private static final String EMAIL = "test@example.com";
    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("Existence Checks")
    class ExistenceTests {
        @Test
        @DisplayName("should check if user exists by username")
        void shouldCheckExistsByUsername() {
            // Arrange
            when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

            // Act
            boolean result = userService.isUserExistsByUsername(USERNAME);

            // Assert
            assertTrue(result);
            verify(userRepository).existsByUsername(USERNAME);
        }

        @Test
        @DisplayName("should check if user exists by email")
        void shouldCheckExistsByEmail() {
            // Arrange
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            // Act
            boolean result = userService.isUserExistsByEmail(EMAIL);

            // Assert
            assertTrue(result);
            verify(userRepository).existsByEmail(EMAIL);
        }

        @Test
        @DisplayName("should check if user exists by id")
        void shouldCheckExistsById() {
            // Arrange
            when(userRepository.existsById(USER_ID)).thenReturn(true);

            // Act
            boolean result = userService.existsById(USER_ID);

            // Assert
            assertTrue(result);
            verify(userRepository).existsById(USER_ID);
        }
    }

    @Nested
    @DisplayName("Persistence Operations")
    class PersistenceTests {
        @Test
        @DisplayName("should insert user successfully")
        void shouldInsertUser() {
            // Arrange
            User user = User.builder().username(USERNAME).build();
            when(userRepository.insert(user)).thenReturn(1);

            // Act
            int result = userService.insert(user);

            // Assert
            assertEquals(1, result);
            verify(userRepository).insert(user);
        }

        @Test
        @DisplayName("should update profile successfully and preserve existing role")
        void shouldUpdateProfileAndPreserveRole() {
            // Arrange
            String originalRole = "ADMIN";
            String newUsername = "newusername";
            String newEmail = "new@example.com";
            User existingUser = User.builder()
                    .id(USER_ID)
                    .username(USERNAME)
                    .email(EMAIL)
                    .role(originalRole)
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
            when(userRepository.update(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(1);

            // Act
            int result = userService.updateProfile(USER_ID, newUsername, newEmail);

            // Assert
            assertEquals(1, result);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).update(userCaptor.capture());
            
            User capturedUser = userCaptor.getValue();
            assertEquals(USER_ID, capturedUser.getId());
            assertEquals(newUsername, capturedUser.getUsername());
            assertEquals(newEmail, capturedUser.getEmail());
            assertEquals(originalRole, capturedUser.getRole()); // Role preserved
        }

        @Test
        @DisplayName("updateProfile should throw ResourceNotFoundException when user not found")
        void updateProfileShouldThrowException() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> userService.updateProfile(USER_ID, "newname", "new@example.com"));
        }
    }

    @Nested
    @DisplayName("Lookup Operations")
    class LookupTests {
        @Test
        @DisplayName("should find user by username")
        void shouldFindByUsername() {
            // Arrange
            User user = User.builder().username(USERNAME).build();
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userService.findByUsername(USERNAME);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(USERNAME, result.get().getUsername());
        }

        @Test
        @DisplayName("should find user by email")
        void shouldFindByEmail() {
            // Arrange
            User user = User.builder().email(EMAIL).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userService.findByEmail(EMAIL);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(EMAIL, result.get().getEmail());
        }

        @Test
        @DisplayName("should find user by id")
        void shouldFindById() {
            // Arrange
            User user = User.builder().id(USER_ID).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userService.findById(USER_ID);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(USER_ID, result.get().getId());
        }

        @Test
        @DisplayName("should find all users")
        void shouldFindAll() {
            // Arrange
            User user = User.builder().id(USER_ID).build();
            when(userRepository.findAll()).thenReturn(List.of(user));

            // Act
            List<User> result = userService.findAll();

            // Assert
            assertEquals(1, result.size());
            verify(userRepository).findAll();
        }
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsernameTests {
        @Test
        @DisplayName("should load UserDetails when user exists")
        void shouldLoadUserDetails() {
            // Arrange
            User user = User.builder()
                    .id(USER_ID)
                    .username(USERNAME)
                    .passwordHash("hashed-password")
                    .email(EMAIL)
                    .build();
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            // Act
            UserDetails result = userService.loadUserByUsername(USERNAME);

            // Assert
            assertNotNull(result);
            assertTrue(result instanceof CustomUserDetails);
            assertEquals(USERNAME, result.getUsername());
            assertEquals(USER_ID, ((CustomUserDetails) result).getId());
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user does not exist")
        void shouldThrowException() {
            // Arrange
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("nonexistent"));
        }
    }
}
