package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedpassword");
    }

    @Test
    void isUserExistsByUsername_ShouldReturnTrueIfUserExists() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When
        boolean exists = userService.isUserExistsByUsername("testuser");

        // Then
        assertThat(exists).isTrue();
        verify(userRepository).existsByUsername("testuser");
    }

    @Test
    void isUserExistsByEmail_ShouldReturnTrueIfUserExists() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When
        boolean exists = userService.isUserExistsByEmail("test@example.com");

        // Then
        assertThat(exists).isTrue();
        verify(userRepository).existsByEmail("test@example.com");
    }

    @Test
    void save_ShouldSaveAndReturnUser() {
        // Given
        when(userRepository.save(user)).thenReturn(user);

        // When
        User savedUser = userService.save(user);

        // Then
        assertThat(savedUser).isEqualTo(user);
        verify(userRepository).save(user);
    }

    @Test
    void findByUsername_ShouldReturnUserIfExists() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When
        Optional<User> foundUser = userService.findByUsername("testuser");

        // Then
        assertThat(foundUser).isPresent().contains(user);
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void findByEmail_ShouldReturnUserIfExists() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        Optional<User> foundUser = userService.findByEmail("test@example.com");

        // Then
        assertThat(foundUser).isPresent().contains(user);
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void findAll_ShouldReturnAllUsers() {
        // Given
        when(userRepository.findAll()).thenReturn(List.of(user));

        // When
        List<User> users = userService.findAll();

        // Then
        assertThat(users).hasSize(1).contains(user);
        verify(userRepository).findAll();
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetailsIfUserExists() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = userService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ShouldThrowExceptionIfUserDoesNotExist() {
        // Given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with username: unknown");
        verify(userRepository).findByUsername("unknown");
    }
}
