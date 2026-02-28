package com.mayureshpatel.pfdataservice.dto.account;

public record AccountTypeDto(
        String code,
        String label,
        boolean isAsset,
        Integer sortOrder,
        boolean isActive,
        String icon,
        String color
) {
}
