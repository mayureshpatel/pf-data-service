package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * A user's financial account (checking, savings, credit card, etc.) and its running balance.
 * {@code currentBalance} is denormalized -- kept in sync incrementally via
 * {@link #applyTransaction}/{@link #undoTransaction} rather than recomputed from the transaction
 * history on every read, since most account reads (dashboards, lists) don't need per-transaction
 * precision and a running total is far cheaper to serve.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Account {

    @EqualsAndHashCode.Include
    private final Long id;
    private final Long userId;
    private final String name;
    private final AccountType type;
    @Builder.Default
    private final BigDecimal currentBalance = BigDecimal.ZERO;
    @ToString.Exclude
    private final Currency currency;
    private final Long version;
    private final String bankCode;
    @ToString.Exclude
    private final TableAudit audit;

    /**
     * Returns a copy of this account with {@code transaction}'s net effect already applied to the
     * balance -- used when the transaction already exists as a domain object (e.g. reversing an
     * edit before re-applying the new values).
     *
     * @param transaction the transaction whose net change should be added to the balance
     * @return a new {@code Account} with the updated balance; this instance is unchanged
     */
    public Account applyTransaction(Transaction transaction) {
        BigDecimal balance = this.currentBalance != null ? this.currentBalance : BigDecimal.ZERO;
        return this.toBuilder()
                .currentBalance(balance.add(transaction.getNetChange()))
                .build();
    }

    /**
     * Returns a copy of this account with the net effect of a not-yet-persisted transaction
     * request applied to the balance -- used on the create path, before the transaction has an id.
     *
     * @param transaction the pending create request whose net change should be added to the balance
     * @return a new {@code Account} with the updated balance; this instance is unchanged
     */
    public Account applyTransaction(TransactionCreateRequest transaction) {
        BigDecimal balance = this.currentBalance != null ? this.currentBalance : BigDecimal.ZERO;
        return this.toBuilder()
                .currentBalance(balance.add(transaction.getNetChange()))
                .build();
    }

    /**
     * The inverse of {@link #applyTransaction(Transaction)} -- used when a persisted transaction
     * is being deleted or is about to be replaced by an edited version.
     *
     * @param transaction the transaction whose net change should be subtracted from the balance
     * @return a new {@code Account} with the updated balance; this instance is unchanged
     */
    public Account undoTransaction(Transaction transaction) {
        BigDecimal balance = this.currentBalance != null ? this.currentBalance : BigDecimal.ZERO;
        return this.toBuilder()
                .currentBalance(balance.subtract(transaction.getNetChange()))
                .build();
    }

    /**
     * The inverse of {@link #applyTransaction(TransactionCreateRequest)}.
     *
     * @param transaction the pending create request whose net change should be subtracted from
     *                     the balance
     * @return a new {@code Account} with the updated balance; this instance is unchanged
     */
    public Account undoTransaction(TransactionCreateRequest transaction) {
        BigDecimal balance = this.currentBalance != null ? this.currentBalance : BigDecimal.ZERO;
        return this.toBuilder()
                .currentBalance(balance.subtract(transaction.getNetChange()))
                .build();
    }
}
