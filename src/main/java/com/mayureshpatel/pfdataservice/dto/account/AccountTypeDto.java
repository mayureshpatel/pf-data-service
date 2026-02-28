package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.Iconography;

public record AccountTypeDto(
        String code,
        String label,
        boolean isAsset,
        Integer sortOrder,
        boolean isActive,
        Iconography iconography
) {
}
