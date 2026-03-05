package com.mayureshpatel.pfdataservice.dto.merchant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("MerchantBreakdownDto Structure Tests")
class MerchantBreakdownDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        MerchantDto merchant = new MerchantDto(1L, 2L, "Original", "Clean");
        BigDecimal total = new BigDecimal("100.50");
        MerchantBreakdownDto dto = new MerchantBreakdownDto(merchant, total);

        assertEquals(merchant, dto.merchant());
        assertEquals(total, dto.total());
    }
}
