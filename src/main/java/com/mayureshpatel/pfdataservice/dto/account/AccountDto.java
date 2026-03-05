package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.dto.currency.CurrencyDto;
import com.mayureshpatel.pfdataservice.dto.user.UserDto;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AccountDto(
        Long id,
        UserDto user,
        String name,
        AccountTypeDto type,
        BigDecimal currentBalance,
        CurrencyDto currency,
        BankName bank
) {
}
