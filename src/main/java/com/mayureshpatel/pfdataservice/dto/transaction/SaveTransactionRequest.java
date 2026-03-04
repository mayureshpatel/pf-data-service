package com.mayureshpatel.pfdataservice.dto.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaveTransactionRequest(
        @NotEmpty(message = "Transactions list must not be empty")
        List<@Valid TransactionDto> transactions,

        @NotBlank(message = "File name cannot be blank")
        String fileName,

        @NotBlank(message = "File hash cannot be blank")
        String fileHash
) {
}
