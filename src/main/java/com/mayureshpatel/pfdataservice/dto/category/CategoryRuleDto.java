package com.mayureshpatel.pfdataservice.dto.category;

import lombok.Builder;

@Builder
public record CategoryRuleDto(
        Long id,
        Long userId,
        String keyword,
        Integer priority,
        CategoryDto category
) {
}
