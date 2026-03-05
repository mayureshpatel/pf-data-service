package com.mayureshpatel.pfdataservice.dto.transaction.recurring;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("RecurringSuggestionDto Structure Tests")
class RecurringSuggestionDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        MerchantDto merchant = MerchantDto.builder().id(1L).build();
        BigDecimal amount = new BigDecimal("100.00");
        Frequency frequency = Frequency.MONTHLY;
        LocalDate lastDate = LocalDate.now().minusMonths(1);
        LocalDate nextDate = LocalDate.now();
        int count = 5;
        double confidence = 0.88;

        RecurringSuggestionDto dto = RecurringSuggestionDto.builder()
                .merchant(merchant)
                .amount(amount)
                .frequency(frequency)
                .lastDate(lastDate)
                .nextDate(nextDate)
                .occurrenceCount(count)
                .confidenceScore(confidence)
                .build();

        assertEquals(merchant, dto.merchant());
        assertEquals(amount, dto.amount());
        assertEquals(frequency, dto.frequency());
        assertEquals(lastDate, dto.lastDate());
        assertEquals(nextDate, dto.nextDate());
        assertEquals(count, dto.occurrenceCount());
        assertEquals(confidence, dto.confidenceScore());
    }
}
