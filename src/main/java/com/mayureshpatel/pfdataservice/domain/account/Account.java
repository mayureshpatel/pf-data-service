package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Account {

    @EqualsAndHashCode.Include
    private final Long id;
    private final Long userId;
    private final String name;
    private final String typeCode;
    @Builder.Default
    private final BigDecimal currentBalance = BigDecimal.ZERO;
    @ToString.Exclude
    private final String currencyCode;
    private final Long version;
    private final String bankCode;
    @ToString.Exclude
    private final TableAudit audit;

    public Account applyTransaction(Transaction transaction) {
        BigDecimal balance = this.currentBalance != null ? this.currentBalance : BigDecimal.ZERO;
        return this.toBuilder()
                .currentBalance(balance.add(transaction.getNetChange()))
                .build();
    }

    public Account undoTransaction(Transaction transaction) {
        BigDecimal balance = this.currentBalance != null ? this.currentBalance : BigDecimal.ZERO;
        return this.toBuilder()
                .currentBalance(balance.subtract(transaction.getNetChange()))
                .build();
    }
}
