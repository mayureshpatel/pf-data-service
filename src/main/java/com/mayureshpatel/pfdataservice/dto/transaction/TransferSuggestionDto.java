package com.mayureshpatel.pfdataservice.dto.transaction;

public record TransferSuggestionDto(
    TransactionDto sourceTransaction,
    TransactionDto targetTransaction,
    double confidenceScore
) {}
