package com.mayureshpatel.pfdataservice.dto.transaction;

import java.util.List;

public record SaveTransactionRequest(
        List<TransactionDto> transactions,
        String fileName,
        String fileHash
) {
}