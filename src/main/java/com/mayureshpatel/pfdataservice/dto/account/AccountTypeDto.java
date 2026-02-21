package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;

public record AccountTypeDto(
        String code,
        String label,
        boolean isAsset,
        Integer sortOrder,
        boolean isActive,
        Iconography iconography
) {

    /**
     * Maps an {@link AccountType} domain object to its corresponding DTO representation.
     *
     * @param accountType The AccountType domain object to be mapped.
     * @return The {@link AccountTypeDto} representation of the provided AccountType.
     */
    public static AccountTypeDto mapToDto(AccountType accountType) {
        return new AccountTypeDto(
                accountType.getCode(),
                accountType.getLabel(),
                accountType.getIsAsset(),
                accountType.getSortOrder(),
                accountType.getIsActive(),
                accountType.getIconography()
        );
    }
}
