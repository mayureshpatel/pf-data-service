package com.mayureshpatel.pfdataservice.repository.transaction.model;

public enum TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER, // Legacy/Generic Transfer
    TRANSFER_IN,
    TRANSFER_OUT,
    ADJUSTMENT
}
