package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;

import java.math.BigDecimal;

/**
 * Represents an account object.
 *
 * @param id             the account id
 * @param userId         the user who owns the account
 * @param name           the account name
 * @param type           the account type code
 * @param currentBalance the current balance
 * @param currency       the account currency
 * @param bankName       the bank name
 */
public record AccountDto(
        Long id,
        Long userId,
        String name,
        AccountType type,
        BigDecimal currentBalance,
        Currency currency,
        BankName bankName
) {
}
