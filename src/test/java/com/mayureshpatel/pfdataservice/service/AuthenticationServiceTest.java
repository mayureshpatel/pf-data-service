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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Verifies {@code AuthenticationService}'s single {@code authenticate} method in {@link AuthenticateTests}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceTest {

    private static final String USERNAME = "testuser";
    private static final String EMAIL = "test@example.com";
    private static final String RAW = "raw";

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    /**
     * {@code authenticate} generates a JWT after delegating credential-checking to the
     * {@code AuthenticationManager}, embedding {@code userId}/{@code email} claims only when the
     * authenticated principal is this codebase's own {@link CustomUserDetails} -- a generic
     * {@code UserDetails} (e.g. from a different auth source) still gets a token, just with an
     * empty claims map. A failed authentication propagates the manager's exception without ever
     * calling {@code userDetailsService}.
     */
    @Nested
    @DisplayName("authenticate")
    class AuthenticateTests {

        @Test
        @DisplayName("should return token when user is an instance of CustomUserDetails")
        void shouldAuthenticateCustomUser() {
            // arrange
            String username = USERNAME;
            String password = "password";
            String token = "jwt-token";
            AuthenticationRequest request = new AuthenticationRequest(username, password, null);

            User user = User.builder()
                    .id(1L)
                    .username(username)
                    .email(EMAIL)
                    .build();
            CustomUserDetails userDetails = new CustomUserDetails(user);

            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtService.generateToken(anyMap(), eq(userDetails))).thenReturn(token);
            IssuedRefreshToken issued = new IssuedRefreshToken(RAW, false, null);
            when(refreshTokenService.issue(argThat(u -> u.getId() == 1L), eq(false))).thenReturn(issued);

            // act
            AuthenticationResult result = authenticationService.authenticate(request);
            AuthenticationResponse response = result.response();

            // assert & verify
            assertNotNull(response);
            assertEquals(token, response.token());
            assertSame(issued, result.refreshToken());
            verify(authenticationManager).authenticate(any());
            verify(jwtService).generateToken(argThat(claims ->
                    claims.get("userId").equals(1L) && EMAIL.equals(claims.get("email"))
            ), eq(userDetails));
        }

        @Test
        @DisplayName("should return token when user is NOT an instance of CustomUserDetails")
        void shouldAuthenticateGenericUser() {
            // arrange
            String username = USERNAME;
            String password = "password";
            String token = "jwt-token";
            AuthenticationRequest request = new AuthenticationRequest(username, password, null);

            UserDetails userDetails = mock(UserDetails.class);
            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtService.generateToken(anyMap(), eq(userDetails))).thenReturn(token);

            // act
            AuthenticationResult result = authenticationService.authenticate(request);
            AuthenticationResponse response = result.response();

            // assert & verify
            assertNotNull(response);
            assertEquals(token, response.token());
            verify(jwtService).generateToken(argThat(Map::isEmpty), eq(userDetails));
            assertNull(result.refreshToken());
            verifyNoInteractions(refreshTokenService);
        }

        @Test
        @DisplayName("should throw exception when authentication fails")
        void shouldThrowOnAuthFailure() {
            // arrange
            AuthenticationRequest request = new AuthenticationRequest("user", "wrong", null);
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

            // act & assert & verify
            assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(request));
            verify(userDetailsService, never()).loadUserByUsername(anyString());
        }
    }

    /** {@code authenticate} passes {@code rememberMe} to the refresh token ({@code null} means false); {@code refresh} and {@code logout} delegate to the refresh-token service. */
    @Nested
    @DisplayName("refresh tokens")
    class RefreshTokenTests {

        private final User user = User.builder().id(1L).username(USERNAME).email(EMAIL).build();

        @Test
        @DisplayName("should issue a remember-me refresh token when rememberMe is true")
        void shouldPassRememberMeTrue() {
            // arrange
            CustomUserDetails userDetails = new CustomUserDetails(user);
            when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
            when(jwtService.generateToken(anyMap(), eq(userDetails))).thenReturn("jwt");

            // act
            authenticationService.authenticate(new AuthenticationRequest(USERNAME, "pw", true));

            // assert & verify
            verify(refreshTokenService).issue(any(User.class), eq(true));
        }

        @Test
        @DisplayName("should build a new access token for the renewed user with the same claims and slid cookie facts")
        void shouldRefresh() {
            // arrange
            when(refreshTokenService.renew(RAW)).thenReturn(Optional.of(new RenewedRefreshToken(user, true, 1200L)));
            when(jwtService.generateToken(anyMap(), any(UserDetails.class))).thenReturn("new-jwt");

            // act
            Optional<AuthenticationResult> result = authenticationService.refresh(RAW);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals("new-jwt", result.get().response().token());
            assertEquals(RAW, result.get().refreshToken().rawToken());
            assertTrue(result.get().refreshToken().rememberMe());
            assertEquals(1200L, result.get().refreshToken().maxAgeSeconds());
            verify(jwtService).generateToken(argThat(claims ->
                    claims.get("userId").equals(1L) && EMAIL.equals(claims.get("email"))), any(UserDetails.class));
        }

        @Test
        @DisplayName("should return empty and build no token when the refresh token is rejected")
        void shouldRejectRefresh() {
            // arrange
            when(refreshTokenService.renew("bad")).thenReturn(Optional.empty());

            // act & assert & verify
            assertTrue(authenticationService.refresh("bad").isEmpty());
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("should revoke the refresh token on logout")
        void shouldLogout() {
            // act
            authenticationService.logout(RAW);

            // assert & verify
            verify(refreshTokenService).revoke(RAW);
        }
    }
}
