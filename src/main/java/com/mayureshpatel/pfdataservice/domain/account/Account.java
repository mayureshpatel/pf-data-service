package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Account {

    private Long id;
    private User user;
    private String name;
    private AccountType type;
    private BigDecimal currentBalance;
    private Currency currency;
    private Long version;
    private BankName bankName;

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
}