package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.model.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TransactionDto {

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String description;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    // Optional: We can add categoryName here later if we implement category lookup on save
    private String categoryName;
}