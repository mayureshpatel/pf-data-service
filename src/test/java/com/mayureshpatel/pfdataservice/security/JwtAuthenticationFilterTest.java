package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserService userService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private UserDetails userDetails;
    @Mock
    private Header jwtHeader;
    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Filter Bypass Scenarios")
    class FilterBypassTests {

        @Test
        @DisplayName("should skip filter when Authorization header is missing")
        void shouldSkipWhenHeaderMissing() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn(null);

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).extractUsername(anyString());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("should skip filter when Authorization header does not start with Bearer ")
        void shouldSkipWhenNoBearer() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Basic dGVzdDp0ZXN0");

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).extractUsername(anyString());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Nested
    @DisplayName("Authentication Logic")
    class AuthenticationLogicTests {

        @Test
        @DisplayName("should authenticate and set security context when token is valid and context empty")
        void shouldSetAuthWhenValidToken() throws ServletException, IOException {
            // Arrange
            String token = "valid.jwt.token";
            String username = "testuser";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn(username);
            when(userService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
            when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication);
            assertEquals(userDetails, authentication.getPrincipal());
            assertNotNull(authentication.getDetails());
            assertInstanceOf(UsernamePasswordAuthenticationToken.class, authentication);
        }

        @Test
        @DisplayName("should not re-authenticate when security context already has authentication")
        void shouldNotReAuthWhenAlreadyAuthenticated() throws ServletException, IOException {
            // Arrange
            String token = "valid.jwt.token";
            String username = "testuser";
            UsernamePasswordAuthenticationToken existingAuth = new UsernamePasswordAuthenticationToken("alreadyAuth", null);
            SecurityContextHolder.getContext().setAuthentication(existingAuth);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn(username);

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(userService, never()).loadUserByUsername(anyString());
            assertSame(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("should skip authentication when username is null")
        void shouldSkipWhenUsernameIsNull() throws ServletException, IOException {
            // Arrange
            String token = "token.with.no.username";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn(null);

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(userService, never()).loadUserByUsername(anyString());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("should not authenticate when token is invalid")
        void shouldNotAuthWhenTokenInvalid() throws ServletException, IOException {
            // Arrange
            String token = "invalid.jwt.token";
            String username = "testuser";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn(username);
            when(userService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Nested
    @DisplayName("Rejection Response Bodies (PF-211)")
    class RejectionResponseBodyTests {

        private StringWriter capturedBody;

        @BeforeEach
        void captureResponseBody() throws IOException {
            capturedBody = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(capturedBody));
        }

        @Test
        @DisplayName("should return a JSON body with a detail field when the token has expired")
        void shouldReturnDetailedBodyOnExpiredToken() throws ServletException, IOException {
            // Arrange
            String token = "expired.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenThrow(new ExpiredJwtException(jwtHeader, claims, "Token expired"));

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
            assertTrue(capturedBody.toString().contains("\"detail\""));
        }

        @Test
        @DisplayName("should return a JSON body with a detail field when the token is malformed or otherwise invalid (PF-211)")
        void shouldReturnDetailedBodyOnMalformedToken() throws ServletException, IOException {
            // Arrange
            String token = "garbage";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("Malformed JWT"));

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
            assertTrue(capturedBody.toString().contains("\"detail\""));
        }
    }
}
