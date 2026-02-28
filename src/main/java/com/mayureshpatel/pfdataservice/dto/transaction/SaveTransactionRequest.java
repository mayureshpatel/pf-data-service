package com.mayureshpatel.pfdataservice.dto.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaveTransactionRequest(
        @NotEmpty(message = "Transactions list must not be empty")
        List<@Valid TransactionDto> transactions,
        String fileName,
        String fileHash
) {
}
