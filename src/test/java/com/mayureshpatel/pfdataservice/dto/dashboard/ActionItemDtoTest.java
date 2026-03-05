package com.mayureshpatel.pfdataservice.dto.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ActionItemDto Unit Tests")
class ActionItemDtoTest {

    @Nested
    @DisplayName("Record Structure")
    class StructureTests {
        @Test
        @DisplayName("should correctly map all fields")
        void shouldPopulateFields() {
            ActionItemDto.ActionType type = ActionItemDto.ActionType.TRANSFER_REVIEW;
            long count = 5;
            String message = "Review transfers";
            String route = "/transfers";

            ActionItemDto dto = new ActionItemDto(type, count, message, route);

            assertEquals(type, dto.type());
            assertEquals(count, dto.count());
            assertEquals(message, dto.message());
            assertEquals(route, dto.route());
        }
    }
}
