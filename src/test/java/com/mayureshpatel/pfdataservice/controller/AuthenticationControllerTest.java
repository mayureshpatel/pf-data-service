package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationRequest;
import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.dto.user.RegistrationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link AuthenticationController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("AuthenticationController Unit Tests")
class AuthenticationControllerTest extends BaseControllerTest {

    private static final String AUTH_TOKEN = "mocked-jwt-token";

    @Nested
    @DisplayName("authenticate")
    class AuthenticateTests {

        @Test
        @DisplayName("POST /authenticate should return token on valid credentials")
        void authenticate_shouldReturnToken() throws Exception {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .username("john_doe")
                    .password("secure_password")
                    .build();

            AuthenticationResponse response = AuthenticationResponse.builder()
                    .token(AUTH_TOKEN)
                    .build();

            // Note: BaseControllerTest has authenticationService mocked as 'authenticationService'
            when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/authenticate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token").value(AUTH_TOKEN));

            verify(authenticationService).authenticate(any(AuthenticationRequest.class));
        }

        @Test
        @DisplayName("POST /authenticate should return 400 Bad Request when validation fails (missing username)")
        void authenticate_shouldReturn400WhenValidationFails() throws Exception {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .password("secure_password")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/authenticate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("username"));
        }
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("POST /register should return token on valid registration")
        void register_shouldReturnToken() throws Exception {
            // Arrange
            RegistrationRequest request = RegistrationRequest.builder()
                    .username("new_user")
                    .email("new@example.com")
                    .password("Pass123!@")
                    .build();

            AuthenticationResponse response = AuthenticationResponse.builder()
                    .token(AUTH_TOKEN)
                    .build();

            when(registrationService.register(any(RegistrationRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token").value(AUTH_TOKEN));

            verify(registrationService).register(any(RegistrationRequest.class));
        }

        @Test
        @DisplayName("POST /register should return 400 Bad Request on invalid email")
        void register_shouldReturn400OnInvalidEmail() throws Exception {
            // Arrange
            RegistrationRequest request = RegistrationRequest.builder()
                    .username("new_user")
                    .email("invalid-email")
                    .password("Pass123!@")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("email"));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("POST /authenticate should return 500 when service fails unexpectedly")
        void authenticate_shouldReturn500WhenServiceFails() throws Exception {
            // Arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .username("john_doe")
                    .password("secure_password")
                    .build();

            when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                    .thenThrow(new RuntimeException("Authentication backend down"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/authenticate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail").value("An unexpected internal error occurred. Please contact support."));
        }
    }
}
