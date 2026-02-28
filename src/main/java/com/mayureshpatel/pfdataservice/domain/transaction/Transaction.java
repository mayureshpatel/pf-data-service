package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private Long id;
    private Account account;
    private Category category;

    private BigDecimal amount;

    private OffsetDateTime transactionDate;
    private OffsetDateTime postDate;
    private String description;
    private Merchant merchant;
    private TransactionType type;

    private TableAudit audit;

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
}