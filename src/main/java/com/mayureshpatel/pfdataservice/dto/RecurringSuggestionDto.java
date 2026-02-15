package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record RecurringSuggestionDto(
    String merchantName,
    BigDecimal amount,
    Frequency frequency,
    LocalDate lastDate,
    LocalDate nextDate,
    int occurrenceCount,
    double confidenceScore
) {}
