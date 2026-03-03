package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@SuperBuilder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Transaction {

    @EqualsAndHashCode.Include
    private Long id;
    private Account account;
    private Category category;
    private BigDecimal amount;
    private OffsetDateTime transactionDate;
    private OffsetDateTime postDate;
    private String description;
    private Merchant merchant;
    private TransactionType type;

    @ToString.Exclude
    private TableAudit audit;

    /**
     * Calculates the net change this transaction applies to an account balance.
     * INCOME/TRANSFER_IN is positive, EXPENSE/TRANSFER_OUT is negative.
     * ADJUSTMENT uses the raw signed amount.
     *
     * @return the net change
     */
    public BigDecimal getNetChange() {
        // return 0 if amount is null
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        // return the raw amount if adjustment
        if (type == TransactionType.ADJUSTMENT) {
            return amount;
        }

        // return the absolute amount if income or transfer in
        if (type == TransactionType.INCOME || type == TransactionType.TRANSFER_IN) {
            return amount.abs();
        }

        // return the negative absolute amount if expense or transfer out
        return amount.abs().negate();
    }
}
