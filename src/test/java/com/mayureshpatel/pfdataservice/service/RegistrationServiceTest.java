package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.dto.user.RegistrationRequest;
import com.mayureshpatel.pfdataservice.exception.UserAlreadyExistsException;
import com.mayureshpatel.pfdataservice.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService Unit Tests")
class RegistrationServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RegistrationService registrationService;

    private static final String USERNAME = "testuser";
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String TOKEN = "jwt-token";

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("should register user successfully when inputs are valid and unique")
        void shouldRegisterSuccessfully() {
            // Arrange
            RegistrationRequest request = RegistrationRequest.builder()
                    .username(USERNAME)
                    .email(EMAIL)
                    .password(PASSWORD)
                    .build();

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(false);
            when(userService.isUserExistsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(jwtService.generateToken(anyMap(), any())).thenReturn(TOKEN);

            // Act
            AuthenticationResponse response = registrationService.register(request);

            // Assert
            assertNotNull(response);
            assertEquals(TOKEN, response.token());
            verify(userService).insert(argThat(user ->
                    user.getUsername().equals(USERNAME) &&
                            user.getEmail().equals(EMAIL) &&
                            user.getPasswordHash().equals(ENCODED_PASSWORD)
            ));
            verify(jwtService).generateToken(argThat(claims ->
                    claims.get("email").equals(EMAIL)
            ), any());
        }

        @Test
        @DisplayName("should throw UserAlreadyExistsException when username already exists")
        void shouldThrowWhenUsernameExists() {
            // Arrange
            RegistrationRequest request = RegistrationRequest.builder()
                    .username(USERNAME)
                    .build();
            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(true);

            // Act & Assert
            UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class, () -> registrationService.register(request));
            assertEquals("Username already exists", ex.getMessage());
            verify(userService, never()).insert(any());
        }

        @Test
        @DisplayName("should throw UserAlreadyExistsException when email already exists")
        void shouldThrowWhenEmailExists() {
            // Arrange
            RegistrationRequest request = RegistrationRequest.builder()
                    .username(USERNAME)
                    .email(EMAIL)
                    .build();
            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(false);
            when(userService.isUserExistsByEmail(EMAIL)).thenReturn(true);

            // Act & Assert
            UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class, () -> registrationService.register(request));
            assertEquals("Email already exists", ex.getMessage());
            verify(userService, never()).insert(any());
        }
    }
}
