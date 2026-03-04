package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;
import com.mayureshpatel.pfdataservice.dto.currency.CurrencyDto;
import com.mayureshpatel.pfdataservice.dto.user.UserDto;

public final class AccountDtoMapper {

    private AccountDtoMapper() {
    }

    public static AccountDto toDto(Account account) {
        if (account == null) {
            return null;
        }

        UserDto user = null;
        AccountTypeDto type = null;
        CurrencyDto currency = null;
        BankName bankName = null;
        if (account.getUserId() != null) {
            user = new UserDto(account.getUserId(), null, null);
        }

        if (account.getTypeCode() != null) {
            type = new AccountTypeDto(
                    account.getTypeCode(),
                    null, false, null, false, null, null
            );
        }

        if (account.getCurrencyCode() != null) {
            currency = new CurrencyDto(account.getCurrencyCode(), null, null, false);
        }

        if (account.getBankCode() != null) {
            bankName = BankName.fromString(account.getBankCode());
        }


        return new AccountDto(
                account.getId(),
                user,
                account.getName(),
                type,
                account.getCurrentBalance(),
                currency,
                bankName
        );
    }
}
