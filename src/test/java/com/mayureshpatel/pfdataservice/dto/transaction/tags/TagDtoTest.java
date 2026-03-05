package com.mayureshpatel.pfdataservice.dto.transaction.tags;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("TagDto Structure Tests")
class TagDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        TagDto dto = new TagDto(1L, 10L, "Urgent", "#FF0000");

        assertEquals(1L, dto.id());
        assertEquals(10L, dto.userId());
        assertEquals("Urgent", dto.name());
        assertEquals("#FF0000", dto.color());
    }
}
