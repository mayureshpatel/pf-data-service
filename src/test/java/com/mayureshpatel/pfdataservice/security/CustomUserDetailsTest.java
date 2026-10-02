package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies {@code CustomUserDetails}'s construction from a domain {@code User} and the {@code UserDetails} contract defaults it implements, one {@code @Nested} class per concern below. */
@DisplayName("CustomUserDetails unit tests")
class CustomUserDetailsTest {

    private static final String USERNAME = "testuser";
    private static final String PASSWORD_HASH = "hashedPassword";
    private static final String EMAIL = "test@example.com";

    /** The constructor copies id/username/password/email straight from the wrapped {@code User}, and defaults to exactly one {@code ROLE_USER} authority when the user's own role isn't set. */
    @Nested
    @DisplayName("Constructor and Mapping")
    class ConstructorAndMappingTest {

        @Test
        @DisplayName("should correctly map all fields from User domain object")
        void shouldMapFieldsFromUser() {
            // arrange
            User user = User.builder()
                    .id(1L)
                    .username(USERNAME)
                    .passwordHash(PASSWORD_HASH)
                    .email(EMAIL)
                    .build();

            // act
            CustomUserDetails userDetails = new CustomUserDetails(user);

            // assert & verify
            assertThat(userDetails.getId()).isEqualTo(1L);
            assertThat(userDetails.getUsername()).isEqualTo(USERNAME);
            assertThat(userDetails.getPassword()).isEqualTo(PASSWORD_HASH);
            assertThat(userDetails.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("should assign ROLE_USER authority by default")
        void shouldAssignDefaultAuthority() {
            // arrange
            User user = User.builder()
                    .id(1L)
                    .username(USERNAME)
                    .passwordHash(PASSWORD_HASH)
                    .email(EMAIL)
                    .build();

            // act
            CustomUserDetails userDetails = new CustomUserDetails(user);
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

            // assert & verify
            assertThat(authorities).hasSize(1);
            assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        }
    }

    /** The four {@code UserDetails} account-status flags ({@code isAccountNonExpired}, {@code isAccountNonLocked}, {@code isCredentialsNonExpired}, {@code isEnabled}) are all hardcoded true -- this application has no account-locking or expiration concept of its own. */
    @Nested
    @DisplayName("UserDetails interface defaults")
    class UserDetailsDefaultsTest {

        @Test
        @DisplayName("should return true for all account status flags")
        void shouldReturnTrueForStatusFlags() {
            // arrange
            User user = User.builder()
                    .id(1L)
                    .username(USERNAME)
                    .passwordHash(PASSWORD_HASH)
                    .email(EMAIL)
                    .build();

            // act
            CustomUserDetails userDetails = new CustomUserDetails(user);

            // assert & verify
            assertThat(userDetails.isAccountNonExpired()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
            assertThat(userDetails.isEnabled()).isTrue();
        }
    }
}
