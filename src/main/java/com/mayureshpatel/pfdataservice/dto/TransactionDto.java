package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.model.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record TransactionDto(
        @NotNull(message = "Date is required")
        LocalDate date,

        String description,

        @NotNull(message = "Amount is required")
        BigDecimal amount,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        String categoryName
) {
}