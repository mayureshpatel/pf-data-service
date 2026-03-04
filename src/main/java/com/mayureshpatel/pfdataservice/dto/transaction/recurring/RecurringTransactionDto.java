package com.mayureshpatel.pfdataservice.dto.transaction.recurring;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record RecurringTransactionDto(
        Long id,
        Long userId,
        
        @NotNull(message = "Account is required")
        AccountDto account,
        
        MerchantDto merchant,
        
        @NotNull(message = "Amount is required")
        BigDecimal amount,
        
        @NotNull(message = "Frequency is required")
        Frequency frequency,
        
        LocalDate lastDate,
        LocalDate nextDate,
        boolean active
) {
}
