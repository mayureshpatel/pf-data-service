package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;

import java.util.List;

/** Converts the {@link AccountType} domain object into its API-facing {@link AccountTypeDto}. */
public final class AccountTypeDtoMapper {

    private AccountTypeDtoMapper() {
    }

    /**
     * @param accountType the domain account type to convert, or {@code null}
     * @return the equivalent DTO, or {@code null} if {@code accountType} was {@code null}
     */
    public static AccountTypeDto toDto(AccountType accountType) {
        if (accountType == null) return null;
        return new AccountTypeDto(
                accountType.getCode(),
                accountType.getLabel(),
                accountType.isAsset(),
                accountType.getSortOrder(),
                accountType.isActive(),
                accountType.getIcon() != null ? accountType.getIcon() : null,
                accountType.getColor() != null ? accountType.getColor() : null
        );
    }

    /**
     * Batch form of {@link #toDto(AccountType)}.
     *
     * @param accountTypes the domain account types to convert
     * @return the equivalent DTOs, in the same order
     */
    public static List<AccountTypeDto> toDto(List<AccountType> accountTypes) {
        return accountTypes.stream().map(AccountTypeDtoMapper::toDto).toList();
    }
}
