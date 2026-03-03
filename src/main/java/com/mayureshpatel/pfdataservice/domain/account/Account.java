package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Builder(toBuilder = true)
@ToString(exclude = "currentBalance")
public class Account {

    private final Long id;
    private final Long userId;
    private final String name;
    private final String typeCode;

    @Builder.Default
    private final BigDecimal currentBalance = BigDecimal.ZERO;
    private final String currencyCode;
    private final Long version;
    private final String bankCode;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Account account = (Account) o;
        return id != null && id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
