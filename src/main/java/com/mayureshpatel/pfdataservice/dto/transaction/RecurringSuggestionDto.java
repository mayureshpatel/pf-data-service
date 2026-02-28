package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record RecurringSuggestionDto(
        MerchantDto merchant,
        BigDecimal amount,
        Frequency frequency,
        LocalDate lastDate,
        LocalDate nextDate,
        int occurrenceCount,
        double confidenceScore
) {
}
