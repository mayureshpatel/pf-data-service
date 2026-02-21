package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.domain.user.User;

import java.math.BigDecimal;

/**
 * Represents an account object.
 *
 * @param id             the account id
 * @param user           the user who owns the account
 * @param name           the account name
 * @param type           the account type code
 * @param currentBalance the current balance
 * @param currency       the account currency
 * @param bankName       the bank name
 */
public record AccountDto(
        Long id,
        User user,
        String name,
        AccountType type,
        BigDecimal currentBalance,
        Currency currency,
        BankName bankName
) {

    /**
     * Maps an {@link Account} to a {@link AccountDto}
     *
     * @param account the account to map
     * @return the mapped {@link AccountDto}
     */
    public static AccountDto fromDomain(Account account) {
        return new AccountDto(
                account.getId(),
                account.getUser(),
                account.getName(),
                account.getType(),
                account.getCurrentBalance(),
                account.getCurrency(),
                account.getBankName()
        );
    }
}
