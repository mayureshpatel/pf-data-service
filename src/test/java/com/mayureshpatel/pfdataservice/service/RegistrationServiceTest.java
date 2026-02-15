package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.dto.RegistrationRequest;
import com.mayureshpatel.pfdataservice.exception.UserAlreadyExistsException;
import com.mayureshpatel.pfdataservice.repository.user.model.User;
import com.mayureshpatel.pfdataservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void register_SuccessfulRegistration_ReturnsJwtToken() {
        // Given
        RegistrationRequest request = RegistrationRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("Password123!")
                .build();

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("newuser");
        savedUser.setEmail("newuser@example.com");
        savedUser.setPasswordHash("encodedPassword");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(anyMap(), any(UserDetails.class))).thenReturn("jwt-token");

        // When
        AuthenticationResponse response = registrationService.register(request);

        // Then
        assertThat(response.token()).isEqualTo("jwt-token");
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername_ThrowsUserAlreadyExistsException() {
        // Given
        RegistrationRequest request = RegistrationRequest.builder()
                .username("existinguser")
                .email("newuser@example.com")
                .password("Password123!")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Username already exists");

        verify(userRepository).existsByUsername("existinguser");
    }

    @Test
    void register_DuplicateEmail_ThrowsUserAlreadyExistsException() {
        // Given
        RegistrationRequest request = RegistrationRequest.builder()
                .username("newuser")
                .email("existing@example.com")
                .password("Password123!")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email already exists");

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("existing@example.com");
    }

    @Test
    void register_PasswordIsEncoded() {
        // Given
        RegistrationRequest request = RegistrationRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("Password123!")
                .build();

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("newuser");
        savedUser.setEmail("newuser@example.com");
        savedUser.setPasswordHash("encodedPassword");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(anyMap(), any(UserDetails.class))).thenReturn("jwt-token");

        // When
        registrationService.register(request);

        // Then
        verify(passwordEncoder).encode("Password123!");
    }

    @Test
    void register_UserSavedWithCorrectFields() {
        // Given
        RegistrationRequest request = RegistrationRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("Password123!")
                .build();

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("newuser");
        savedUser.setEmail("newuser@example.com");
        savedUser.setPasswordHash("encodedPassword");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(anyMap(), any(UserDetails.class))).thenReturn("jwt-token");

        // When
        registrationService.register(request);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getUsername()).isEqualTo("newuser");
        assertThat(capturedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(capturedUser.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(capturedUser.getLastUpdatedBy()).isEqualTo("newuser");
    }
}
