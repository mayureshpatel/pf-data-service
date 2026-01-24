package com.mayureshpatel.pfdataservice.dto;

public record TransferSuggestionDto(
    TransactionDto sourceTransaction,
    TransactionDto targetTransaction,
    double confidenceScore
) {}
