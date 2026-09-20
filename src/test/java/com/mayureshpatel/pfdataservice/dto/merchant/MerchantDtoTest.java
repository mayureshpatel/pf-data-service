package com.mayureshpatel.pfdataservice.dto.merchant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("MerchantDto Structure Tests")
class MerchantDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        MerchantDto dto = new MerchantDto(1L, 2L, "Starbucks", "Atlanta", "GA", "30301", "USA");

        assertEquals(1L, dto.id());
        assertEquals(2L, dto.userId());
        assertEquals("Starbucks", dto.name());
        assertEquals("Atlanta", dto.city());
        assertEquals("GA", dto.state());
        assertEquals("30301", dto.postalCode());
        assertEquals("USA", dto.country());
    }
}
