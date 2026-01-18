package com.mayureshpatel.pfdataservice.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaveTransactionRequest(
        @NotEmpty(message = "Transaction list cannot be empty")
        List<TransactionDto> transactions,
        String fileName,
        String fileHash
) {
}