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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService unit tests")
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService userService;

    private static final String USERNAME = "john_doe";
    private static final String EMAIL = "john@example.com";

    private User buildUser(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$hash");
        return user;
    }

    @Nested
    @DisplayName("isUserExistsByUsername()")
    class IsUserExistsByUsernameTests {

        @Test
        @DisplayName("should return true when username exists in repository")
        void isUserExistsByUsername_exists_returnsTrue() {
            when(repository.existsByUsername(USERNAME)).thenReturn(true);

            assertThat(userService.isUserExistsByUsername(USERNAME)).isTrue();
            verify(repository).existsByUsername(USERNAME);
        }

        @Test
        @DisplayName("should return false when username does not exist in repository")
        void isUserExistsByUsername_notExists_returnsFalse() {
            when(repository.existsByUsername(USERNAME)).thenReturn(false);

            assertThat(userService.isUserExistsByUsername(USERNAME)).isFalse();
        }
    }

    @Nested
    @DisplayName("isUserExistsByEmail()")
    class IsUserExistsByEmailTests {

        @Test
        @DisplayName("should return true when email exists in repository")
        void isUserExistsByEmail_exists_returnsTrue() {
            when(repository.existsByEmail(EMAIL)).thenReturn(true);

            assertThat(userService.isUserExistsByEmail(EMAIL)).isTrue();
            verify(repository).existsByEmail(EMAIL);
        }

        @Test
        @DisplayName("should return false when email does not exist in repository")
        void isUserExistsByEmail_notExists_returnsFalse() {
            when(repository.existsByEmail(EMAIL)).thenReturn(false);

            assertThat(userService.isUserExistsByEmail(EMAIL)).isFalse();
        }
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("should delegate to repository and return the saved user")
        void save_delegatesToRepository_returnsSavedUser() {
            User user = buildUser(1L, USERNAME, EMAIL);
            when(repository.save(user)).thenReturn(user);

            User result = userService.save(user);

            assertThat(result).isSameAs(user);
            verify(repository).save(user);
        }
    }

    @Nested
    @DisplayName("findByUsername()")
    class FindByUsernameTests {

        @Test
        @DisplayName("should return Optional containing user when username is found")
        void findByUsername_found_returnsOptionalWithUser() {
            User user = buildUser(1L, USERNAME, EMAIL);
            when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            Optional<User> result = userService.findByUsername(USERNAME);

            assertThat(result).contains(user);
        }

        @Test
        @DisplayName("should return empty Optional when username is not found")
        void findByUsername_notFound_returnsEmpty() {
            when(repository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            Optional<User> result = userService.findByUsername(USERNAME);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmailTests {

        @Test
        @DisplayName("should return Optional containing user when email is found")
        void findByEmail_found_returnsOptionalWithUser() {
            User user = buildUser(1L, USERNAME, EMAIL);
            when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            Optional<User> result = userService.findByEmail(EMAIL);

            assertThat(result).contains(user);
        }

        @Test
        @DisplayName("should return empty Optional when email is not found")
        void findByEmail_notFound_returnsEmpty() {
            when(repository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            Optional<User> result = userService.findByEmail(EMAIL);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("should return all users from repository")
        void findAll_returnsAllUsers() {
            User u1 = buildUser(1L, "user1", "u1@test.com");
            User u2 = buildUser(2L, "user2", "u2@test.com");
            when(repository.findAll()).thenReturn(List.of(u1, u2));

            List<User> result = userService.findAll();

            assertThat(result).hasSize(2).containsExactly(u1, u2);
        }

        @Test
        @DisplayName("should return empty list when no users exist")
        void findAll_noUsers_returnsEmptyList() {
            when(repository.findAll()).thenReturn(List.of());

            List<User> result = userService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("should return CustomUserDetails wrapping the user when user is found")
        void loadUserByUsername_found_returnsCustomUserDetails() {
            User user = buildUser(1L, USERNAME, EMAIL);
            when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            UserDetails result = userService.loadUserByUsername(USERNAME);

            assertThat(result).isInstanceOf(CustomUserDetails.class);
            assertThat(result.getUsername()).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user is not found")
        void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
            when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.loadUserByUsername("ghost"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("ghost");
        }

        @Test
        @DisplayName("should use the exact username string to query the repository")
        void loadUserByUsername_passesExactUsernameToRepository() {
            User user = buildUser(42L, USERNAME, EMAIL);
            when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            userService.loadUserByUsername(USERNAME);

            verify(repository).findByUsername(USERNAME);
        }
    }
}
