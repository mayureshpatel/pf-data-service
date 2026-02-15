package com.mayureshpatel.pfdataservice.domain.transaction;

public enum TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER, // Legacy/Generic Transfer
    TRANSFER_IN,
    TRANSFER_OUT,
    ADJUSTMENT
}
