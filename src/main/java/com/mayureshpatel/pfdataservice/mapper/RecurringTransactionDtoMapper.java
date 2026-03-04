package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;

public final class RecurringTransactionDtoMapper {

    private RecurringTransactionDtoMapper() {}

    public static RecurringTransactionDto toDto(RecurringTransaction recurring) {
        if (recurring == null) return null;
        return new RecurringTransactionDto(
                recurring.getId(),
                recurring.getUser() != null ? recurring.getUser().getId() : null,
                AccountDtoMapper.toDto(recurring.getAccount()),
                MerchantDtoMapper.toDto(recurring.getMerchant()),
                recurring.getAmount(),
                recurring.getFrequency(),
                recurring.getLastDate(),
                recurring.getNextDate(),
                recurring.isActive()
        );
    }
}
