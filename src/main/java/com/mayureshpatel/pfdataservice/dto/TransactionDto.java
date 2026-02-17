package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record TransactionDto(
        Long id,

        @NotNull(message = "Date is required")
        OffsetDateTime date,

        OffsetDateTime postDate,

        String description,

        String originalVendorName,

        String vendorName,

        @NotNull(message = "Amount is required")
        BigDecimal amount,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        String categoryName,

        Long accountId
) {
}