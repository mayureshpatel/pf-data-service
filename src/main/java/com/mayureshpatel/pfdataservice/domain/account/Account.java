package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Account {

    private Long id;
    @ToString.Exclude
    private User user;
    private String name;
    @ToString.Exclude
    private AccountType type;
    private BigDecimal currentBalance;
    @ToString.Exclude
    private Currency currency;
    private Long version;
    private BankName bankName;

    @ToString.Exclude
    private TableAudit audit;

    /**
     * Apply a transaction to the account, updating the current balance.
     *
     * @param transaction the transaction to apply
     */
    public void applyTransaction(Transaction transaction) {
        if (this.currentBalance == null) {
            this.currentBalance = BigDecimal.ZERO;
        }
        this.currentBalance = this.currentBalance.add(transaction.getNetChange());
    }

    /**
     * Undo a transaction by subtracting its net change from the current balance.
     *
     * @param transaction the transaction to undo
     */
    public void undoTransaction(Transaction transaction) {
        if (this.currentBalance == null) {
            this.currentBalance = BigDecimal.ZERO;
        }
        this.currentBalance = this.currentBalance.subtract(transaction.getNetChange());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return id != null && id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
