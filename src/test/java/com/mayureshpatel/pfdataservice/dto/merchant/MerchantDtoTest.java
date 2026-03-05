package com.mayureshpatel.pfdataservice.dto.merchant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("MerchantDto Structure Tests")
class MerchantDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        MerchantDto dto = new MerchantDto(1L, 2L, "Original", "Clean");

        assertEquals(1L, dto.id());
        assertEquals(2L, dto.userId());
        assertEquals("Original", dto.originalName());
        assertEquals("Clean", dto.cleanName());
    }
}
