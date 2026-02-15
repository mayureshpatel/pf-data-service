package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.vendor.Vendor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private Long id;
    private BigDecimal amount;

    private OffsetDateTime transactionDate;
    private LocalDate postDate;
    private String description;
    private String originalVendorName;
    private Vendor vendor;
    private TransactionType type;
    private Account account;
    private Category category;
    private Set<Tag> tags = new HashSet<>();

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