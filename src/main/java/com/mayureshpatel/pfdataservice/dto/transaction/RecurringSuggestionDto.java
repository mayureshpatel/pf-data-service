package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record RecurringSuggestionDto(
        Merchant merchant,
        BigDecimal amount,
        Frequency frequency,
        OffsetDateTime lastDate,
        OffsetDateTime nextDate,
        int occurrenceCount,
        double confidenceScore
) {
}
