package com.mayureshpatel.pfdataservice.dto.merchant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies {@code MerchantDescriptionLinkDto}'s record accessors map their constructor arguments 1:1. */
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
