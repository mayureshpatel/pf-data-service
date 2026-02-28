package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;

public final class AccountDtoMapper {

    private AccountDtoMapper() {}

    public static AccountDto toDto(Account account) {
        if (account == null) return null;
        return new AccountDto(
                account.getId(),
                account.getUser() != null ? account.getUser().getId() : null,
                account.getName(),
                account.getType() != null ? account.getType().getCode() : null,
                account.getType() != null ? account.getType().getLabel() : null,
                account.getCurrentBalance(),
                account.getCurrency() != null ? account.getCurrency().getCode() : null,
                account.getCurrency() != null ? account.getCurrency().getSymbol() : null,
                account.getBankName() != null ? account.getBankName().getDisplayName() : null
        );
    }
}
