package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;

public final class AccountTypeDtoMapper {

    private AccountTypeDtoMapper() {}

    public static AccountTypeDto toDto(AccountType accountType) {
        if (accountType == null) return null;
        return new AccountTypeDto(
                accountType.getCode(),
                accountType.getLabel(),
                accountType.isAsset(),
                accountType.getSortOrder(),
                accountType.isActive(),
                accountType.getIconography() != null ? accountType.getIconography().getIcon() : null,
                accountType.getIconography() != null ? accountType.getIconography().getColor() : null
        );
    }
}
