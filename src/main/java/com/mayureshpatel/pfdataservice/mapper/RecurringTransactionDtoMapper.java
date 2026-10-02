package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;

/**
 * Converts the {@link RecurringTransaction} domain object into its API-facing
 * {@link RecurringTransactionDto}.
 */
public final class RecurringTransactionDtoMapper {

    private RecurringTransactionDtoMapper() {
    }

    /**
     * @param recurring the domain recurring-transaction template to convert, or {@code null}
     * @return the equivalent DTO, or {@code null} if {@code recurring} was {@code null}
     */
    public static RecurringTransactionDto toDto(RecurringTransaction recurring) {
        if (recurring == null) return null;
        return new RecurringTransactionDto(
                recurring.getId(),
                recurring.getUserId() != null ? recurring.getUserId() : null,
                AccountDtoMapper.toDto(recurring.getAccount()),
                MerchantDtoMapper.toDto(recurring.getMerchant()),
                recurring.getAmount(),
                Frequency.fromCode(recurring.getFrequency()),
                recurring.getLastDate(),
                recurring.getNextDate(),
                recurring.isActive()
        );
    }
}
