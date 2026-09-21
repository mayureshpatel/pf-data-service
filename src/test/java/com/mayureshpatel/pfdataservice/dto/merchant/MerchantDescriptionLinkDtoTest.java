package com.mayureshpatel.pfdataservice.dto.merchant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("MerchantDescriptionLinkDto Structure Tests")
class MerchantDescriptionLinkDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        MerchantDescriptionLinkDto dto = new MerchantDescriptionLinkDto(1L, 7L, "STARBUCKS #1");

        assertEquals(1L, dto.id());
        assertEquals(7L, dto.merchantId());
        assertEquals("STARBUCKS #1", dto.description());
    }
}
