package com.mayureshpatel.pfdataservice.domain.transaction;

/**
 * The kind of financial event a {@link Transaction} represents, and specifically how its
 * {@code amount} should be signed when applied to an account balance (see
 * {@link Transaction#getNetChange()}). {@code TRANSFER_IN}/{@code TRANSFER_OUT} are the two legs
 * actually stored for a transfer between two of the user's own accounts; plain {@code TRANSFER}
 * is never itself stored on a transaction -- it's a filter-only value that
 * {@code TransactionSpecification} expands into all three transfer variants, letting a caller ask
 * for "any transfer" without knowing about the leg-level distinction.
 */
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
