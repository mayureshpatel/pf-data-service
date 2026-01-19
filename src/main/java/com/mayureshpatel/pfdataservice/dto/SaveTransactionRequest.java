package com.mayureshpatel.pfdataservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaveTransactionRequest(
        @NotEmpty(message = "Transaction list cannot be empty")
        List<@Valid TransactionDto> transactions,
        String fileName,
        String fileHash
) {
}