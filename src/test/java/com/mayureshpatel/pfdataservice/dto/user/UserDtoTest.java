package com.mayureshpatel.pfdataservice.dto.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies {@code UserDto}'s record accessor in {@link StructureTests}. */
@DisplayName("UserDto structure tests")
class UserDtoTest {

    /** {@code UserDto}'s record accessors map their constructor arguments 1:1. */
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
