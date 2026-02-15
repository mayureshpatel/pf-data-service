package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Account {

    private Long id;
    private String name;
    private String type;
    private BigDecimal currentBalance;
    private String currencyCode;
    private Long version;

    private BankName bankName;
    private User user;
    private TableAudit audit;

    public void applyTransaction(Transaction transaction) {
        if (this.currentBalance == null) this.currentBalance = BigDecimal.ZERO;
        this.currentBalance = this.currentBalance.add(transaction.getNetChange());
    }

    public void undoTransaction(Transaction transaction) {
        if (this.currentBalance == null) this.currentBalance = BigDecimal.ZERO;
        this.currentBalance = this.currentBalance.subtract(transaction.getNetChange());
    }
}