package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationRequest;
import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Nested
    @DisplayName("authenticate")
    class AuthenticateTests {

        @Test
        @DisplayName("should return token when user is an instance of CustomUserDetails")
        void shouldAuthenticateCustomUser() {
            // Arrange
            String username = "testuser";
            String password = "password";
            String token = "jwt-token";
            AuthenticationRequest request = new AuthenticationRequest(username, password);

            User user = User.builder()
                    .id(1L)
                    .username(username)
                    .email("test@example.com")
                    .build();
            CustomUserDetails userDetails = new CustomUserDetails(user);

            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtService.generateToken(anyMap(), eq(userDetails))).thenReturn(token);

            // Act
            AuthenticationResponse response = authenticationService.authenticate(request);

            // Assert
            assertNotNull(response);
            assertEquals(token, response.token());
            verify(authenticationManager).authenticate(any());
            verify(jwtService).generateToken(argThat(claims ->
                    claims.get("userId").equals(1L) && claims.get("email").equals("test@example.com")
            ), eq(userDetails));
        }

        @Test
        @DisplayName("should return token when user is NOT an instance of CustomUserDetails")
        void shouldAuthenticateGenericUser() {
            // Arrange
            String username = "testuser";
            String password = "password";
            String token = "jwt-token";
            AuthenticationRequest request = new AuthenticationRequest(username, password);

            UserDetails userDetails = mock(UserDetails.class);
            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtService.generateToken(anyMap(), eq(userDetails))).thenReturn(token);

            // Act
            AuthenticationResponse response = authenticationService.authenticate(request);

            // Assert
            assertNotNull(response);
            assertEquals(token, response.token());
            verify(jwtService).generateToken(argThat(Map::isEmpty), eq(userDetails));
        }

        @Test
        @DisplayName("should throw exception when authentication fails")
        void shouldThrowOnAuthFailure() {
            // Arrange
            AuthenticationRequest request = new AuthenticationRequest("user", "wrong");
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

            // Act & Assert
            assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(request));
            verify(userDetailsService, never()).loadUserByUsername(anyString());
        }
    }
}
