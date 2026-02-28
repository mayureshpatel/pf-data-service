package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record RecurringTransactionDto(
        Long id,
        Long userId,
        AccountDto account,
        MerchantDto merchant,
        BigDecimal amount,
        Frequency frequency,
        OffsetDateTime lastDate,
        OffsetDateTime nextDate,
        boolean active
) {

    /**
     * Maps a {@link RecurringTransaction} domain object to its corresponding DTO representation.
     *
     * @param recurringTransaction The RecurringTransaction domain object to be mapped.
     * @return The {@link RecurringTransactionDto} representation of the provided RecurringTransaction.
     */
    public static RecurringTransactionDto mapToDto(RecurringTransaction recurringTransaction) {
        return new RecurringTransactionDto(
                recurringTransaction.getId(),
                recurringTransaction.getUser().getId(),
                recurringTransaction.getAccount().toDto(),
                MerchantDto.mapToDto(recurringTransaction.getMerchant()),
                recurringTransaction.getAmount(),
                recurringTransaction.getFrequency(),
                recurringTransaction.getLastDate(),
                recurringTransaction.getNextDate(),
                recurringTransaction.isActive()
        );
    }
}
