package com.mayureshpatel.pfdataservice.domain.account;

/**
 * How an {@link Account}'s {@code currentBalance} is maintained. {@code DERIVED} accounts have the
 * balance kept in sync from their transactions; {@code MANUAL} accounts (investment, retirement,
 * loan and similar) have the balance entered by hand, so transactions never move it.
 */
public enum BalanceMode {
    DERIVED,
    MANUAL
}
