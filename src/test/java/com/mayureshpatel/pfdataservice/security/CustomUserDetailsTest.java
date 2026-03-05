package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomUserDetails unit tests")
class CustomUserDetailsTest {

    @Nested
    @DisplayName("Constructor and Mapping")
    class ConstructorAndMappingTest {

        @Test
        @DisplayName("should correctly map all fields from User domain object")
        void shouldMapFieldsFromUser() {
            // Arrange
            User user = User.builder()
                    .id(1L)
                    .username("testuser")
                    .passwordHash("hashedPassword")
                    .email("test@example.com")
                    .build();

            // Act
            CustomUserDetails userDetails = new CustomUserDetails(user);

            // Assert
            assertThat(userDetails.getId()).isEqualTo(1L);
            assertThat(userDetails.getUsername()).isEqualTo("testuser");
            assertThat(userDetails.getPassword()).isEqualTo("hashedPassword");
            assertThat(userDetails.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should assign ROLE_USER authority by default")
        void shouldAssignDefaultAuthority() {
            // Arrange
            User user = User.builder()
                    .id(1L)
                    .username("testuser")
                    .passwordHash("hashedPassword")
                    .email("test@example.com")
                    .build();

            // Act
            CustomUserDetails userDetails = new CustomUserDetails(user);
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

            // Assert
            assertThat(authorities).hasSize(1);
            assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("UserDetails interface defaults")
    class UserDetailsDefaultsTest {

        @Test
        @DisplayName("should return true for all account status flags")
        void shouldReturnTrueForStatusFlags() {
            // Arrange
            User user = User.builder()
                    .id(1L)
                    .username("testuser")
                    .passwordHash("hashedPassword")
                    .email("test@example.com")
                    .build();

            // Act
            CustomUserDetails userDetails = new CustomUserDetails(user);

            // Assert
            assertThat(userDetails.isAccountNonExpired()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
            assertThat(userDetails.isEnabled()).isTrue();
        }
    }
}
