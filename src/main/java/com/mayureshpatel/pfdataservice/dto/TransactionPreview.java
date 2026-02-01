package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.model.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record TransactionPreview(
        LocalDate date,
        LocalDate postDate,
        String description,
        BigDecimal amount,
        TransactionType type,
        String suggestedCategory,
        String vendorName
) {
}
