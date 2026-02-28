package com.mayureshpatel.pfdataservice.dto.category;

public record CategoryRuleDto(
        Long id,
        Long userId,
        String keyword,
        Integer priority,
        CategoryDto category
) {
}
