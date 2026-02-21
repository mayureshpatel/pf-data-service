package com.mayureshpatel.pfdataservice.dto.category;

import lombok.Builder;

@Builder
public record CategoryRuleDto(
        Long id,
        String keyword,
        Integer priority,
        CategoryDto category
) {
}
