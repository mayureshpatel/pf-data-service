package com.mayureshpatel.pfdataservice.dto.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserDto structure tests")
class UserDtoTest {

    @Nested
    @DisplayName("Structure")
    class StructureTests {

        @Test
        @DisplayName("should correctly map all fields via constructor")
        void shouldPopulateFieldsViaConstructor() {
            Long id = 1L;
            String username = "testuser";
            String email = "test@example.com";

            UserDto dto = new UserDto(id, username, email);

            assertThat(dto.id()).isEqualTo(id);
            assertThat(dto.username()).isEqualTo(username);
            assertThat(dto.email()).isEqualTo(email);
        }
    }
}
