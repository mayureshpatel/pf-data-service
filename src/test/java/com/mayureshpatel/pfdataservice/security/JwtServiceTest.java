package com.mayureshpatel.pfdataservice.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
    }

    private UserDetails createMockUser(String username) {
        return User.builder()
                .username(username)
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGenerationTests {

        @Test
        @DisplayName("should generate a valid JWT token for a user")
        void shouldGenerateToken() {
            // Arrange
            UserDetails user = createMockUser("testuser");

            // Act
            String token = jwtService.generateToken(user);

            // Assert
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertEquals("testuser", jwtService.extractUsername(token));
        }

        @Test
        @DisplayName("should generate a valid JWT token for a user (single arg)")
        void shouldGenerateTokenSingleArg() {
            // Arrange
            UserDetails user = createMockUser("testuser2");

            // Act
            String token = jwtService.generateToken(user);

            // Assert
            assertNotNull(token);
            assertEquals("testuser2", jwtService.extractUsername(token));
        }

        @Test
        @DisplayName("should generate a token with extra claims")
        void shouldGenerateTokenWithExtraClaims() {
            // Arrange
            UserDetails user = createMockUser("testuser");
            Map<String, Object> extraClaims = Map.of("role", "ADMIN", "id", 123);

            // Act
            String token = jwtService.generateToken(extraClaims, user);

            // Assert
            assertNotNull(token);
            assertEquals("ADMIN", jwtService.extractClaim(token, claims -> claims.get("role")));
            assertEquals(123, (Integer) jwtService.extractClaim(token, claims -> claims.get("id")));
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("should return true for a valid token and matching user")
        void shouldBeValid() {
            // Arrange
            UserDetails user = createMockUser("john");
            String token = jwtService.generateToken(user);

            // Act
            boolean isValid = jwtService.isTokenValid(token, user);

            // Assert
            assertTrue(isValid);
        }

        @Test
        @DisplayName("should return false when username does not match")
        void shouldBeInvalidForDifferentUser() {
            // Arrange
            UserDetails user1 = createMockUser("user1");
            UserDetails user2 = createMockUser("user2");
            String token = jwtService.generateToken(user1);

            // Act
            boolean isValid = jwtService.isTokenValid(token, user2);

            // Assert
            assertFalse(isValid);
        }

        @Test
        @DisplayName("should throw ExpiredJwtException for an expired token")
        void shouldThrowOnExpiredToken() {
            // Arrange
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L); // Expired 1 second ago
            UserDetails user = createMockUser("john");
            String token = jwtService.generateToken(user);

            // Act & Assert
            assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(token, user));
        }
    }

    @Nested
    @DisplayName("Claim Extraction")
    class ClaimExtractionTests {

        @Test
        @DisplayName("should extract subject as username")
        void shouldExtractUsername() {
            // Arrange
            String token = jwtService.generateToken(createMockUser("bob"));

            // Act
            String username = jwtService.extractUsername(token);

            // Assert
            assertEquals("bob", username);
        }
    }
}
