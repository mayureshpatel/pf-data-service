package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;

public record CategoryDto(
        Long id,
        Long userId,
        String name,
        CategoryType categoryType,
        CategoryDto parent,
        Iconography iconography
) {
}
