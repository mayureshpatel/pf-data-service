package com.mayureshpatel.pfdataservice.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("RuleChangePreviewDto Structure Tests")
class RuleChangePreviewDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        RuleChangePreviewDto dto = new RuleChangePreviewDto("Desc", "Old", "New");

        assertEquals("Desc", dto.description());
        assertEquals("Old", dto.oldValue());
        assertEquals("New", dto.newValue());
    }
}
