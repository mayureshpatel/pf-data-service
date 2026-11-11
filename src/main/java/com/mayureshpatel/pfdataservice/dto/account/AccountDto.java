package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.account.BalanceMode;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.dto.currency.CurrencyDto;
import com.mayureshpatel.pfdataservice.dto.user.UserDto;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * API view of an account. {@code moneyIn} and {@code gain} are only populated for a
 * {@link BalanceMode#MANUAL} account: {@code moneyIn} is the net amount the user has put in
 * (contributions less transfers out and expenses) and {@code gain} is the latest recorded balance
 * minus that, or {@code null} while no balance has been recorded.
 */
@Builder
public record AccountDto(
        Long id,
        UserDto user,
        String name,
        AccountTypeDto type,
        BigDecimal currentBalance,
        CurrencyDto currency,
        BankName bank,
        Long version,
        BalanceMode balanceMode,
        BigDecimal moneyIn,
        BigDecimal gain
) {
}
