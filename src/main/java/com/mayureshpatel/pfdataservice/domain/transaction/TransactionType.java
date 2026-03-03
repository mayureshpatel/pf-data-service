package com.mayureshpatel.pfdataservice.domain.transaction;

public enum TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    TRANSFER_IN,
    TRANSFER_OUT,
    ADJUSTMENT;

    /**
     * Returns true if the transaction is an expense.
     *
     * @return true if the transaction is an expense, false otherwise.
     */
    public boolean isExpense() {
        return this == EXPENSE || this == TRANSFER_OUT;
    }

    /**
     * Returns true if the transaction is a transfer.
     *
     * @return true if the transaction is a transfer, false otherwise.
     */
    public boolean isTransfer() {
        return this == TRANSFER;
    }

    /**
     * Returns true if the transaction is an income.
     *
     * @return true if the transaction is an income, false otherwise.
     */
    public boolean isIncome() {
        return this == INCOME || this == TRANSFER_IN;
    }

    /**
     * Returns true if the transaction is an adjustment.
     *
     * @return true if the transaction is an adjustment, false otherwise.
     */
    public boolean isAdjustment() {
        return this == ADJUSTMENT;
    }
}
