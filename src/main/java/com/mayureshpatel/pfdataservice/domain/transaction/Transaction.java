package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.SoftDeleteAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private Long id;
    @ToString.Exclude
    private Account account;
    @ToString.Exclude
    private Category category;

    private BigDecimal amount;

    private OffsetDateTime transactionDate;
    private OffsetDateTime postDate;
    private String description;
    @ToString.Exclude
    private Merchant merchant;
    private TransactionType type;

    @ToString.Exclude
    private SoftDeleteAudit audit;

    /**
     * Calculates the net change this transaction applies to an account balance.
     * INCOME/TRANSFER_IN is positive, EXPENSE/TRANSFER_OUT is negative.
     * ADJUSTMENT uses the raw signed amount.
     */
    public BigDecimal getNetChange() {
        if (amount == null) return BigDecimal.ZERO;
        if (type == TransactionType.ADJUSTMENT) {
            return amount;
        }
        if (type == TransactionType.INCOME || type == TransactionType.TRANSFER_IN) {
            return amount.abs();
        }
        return amount.abs().negate();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
