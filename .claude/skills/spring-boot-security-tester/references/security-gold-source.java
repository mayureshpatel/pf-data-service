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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gold Standard examples for Security component unit testing.
 * Demonstrates testing for logic-heavy services like JwtService.
 */
@DisplayName("Security Gold Standard Tests")
class SecurityGoldStandardTest {

    private JwtService jwtService;
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
    }

    @Nested
    @DisplayName("Token Logic")
    class TokenLogicTests {

        @Test
        @DisplayName("should generate and parse valid token")
        void shouldGenerateAndParse() {
            // arrange
            UserDetails user = User.builder().username("test").password("p").authorities(Collections.emptyList()).build();

            // act
            String token = jwtService.generateToken(user);
            String extracted = jwtService.extractUsername(token);

            // assert & verify
            assertEquals("test", extracted);
            assertTrue(jwtService.isTokenValid(token, user));
        }

        @Test
        @DisplayName("should fail validation for incorrect user")
        void shouldFailForIncorrectUser() {
            // arrange
            UserDetails user1 = User.builder().username("u1").password("p").authorities(Collections.emptyList()).build();
            UserDetails user2 = User.builder().username("u2").password("p").authorities(Collections.emptyList()).build();
            String token = jwtService.generateToken(user1);

            // act
            boolean isValid = jwtService.isTokenValid(token, user2);

            // assert & verify
            assertFalse(isValid);
        }

        @Test
        @DisplayName("should throw on expired token")
        void shouldThrowOnExpired() {
            // arrange
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
            UserDetails user = User.builder().username("u").password("p").authorities(Collections.emptyList()).build();
            String token = jwtService.generateToken(user);

            // act & assert
            assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(token, user));
        }
    }
}
