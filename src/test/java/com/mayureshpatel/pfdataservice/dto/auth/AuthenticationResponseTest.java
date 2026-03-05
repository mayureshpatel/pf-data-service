package com.mayureshpatel.pfdataservice.dto.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthenticationResponse structure tests")
class AuthenticationResponseTest {

    @Nested
    @DisplayName("Structure")
    class StructureTests {

        @Test
        @DisplayName("should correctly map all fields using builder")
        void shouldPopulateFieldsViaBuilder() {
            String token = "test-token";
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .token(token)
                    .build();

            assertThat(response.token()).isEqualTo(token);
        }

        @Test
        @DisplayName("should correctly map all fields using constructor")
        void shouldPopulateFieldsViaConstructor() {
            String token = "test-token";
            AuthenticationResponse response = new AuthenticationResponse(token);

            assertThat(response.token()).isEqualTo(token);
        }
    }
}
