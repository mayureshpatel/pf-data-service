package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.transaction.RecurringTransactionDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransaction {

    private Long id;
    private User user;
    private Account account;

    private Merchant merchant;
    private BigDecimal amount;
    private Frequency frequency;

    private OffsetDateTime lastDate;
    private OffsetDateTime nextDate;
    private boolean active = true;

    private TableAudit audit;

    public RecurringTransactionDto toDto() {
        return new RecurringTransactionDto(
                id,
                user.getId(),
                account.toDto(),
                merchant.toDto(),
                amount,
                frequency,
                lastDate,
                nextDate,
                active
        );
    }
}
