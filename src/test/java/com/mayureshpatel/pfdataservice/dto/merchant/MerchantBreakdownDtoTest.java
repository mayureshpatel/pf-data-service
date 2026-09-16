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
        Long representativeMerchantId = 1L;
        String displayName = "Clean";
        BigDecimal total = new BigDecimal("100.50");
        MerchantBreakdownDto dto = new MerchantBreakdownDto(representativeMerchantId, displayName, total);

        assertEquals(representativeMerchantId, dto.representativeMerchantId());
        assertEquals(displayName, dto.displayName());
        assertEquals(total, dto.total());
    }
}
