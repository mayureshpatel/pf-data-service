package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import lombok.Builder;

@Builder
public record CategoryRuleDto(
        Long id,
        String keyword,
        Integer priority,
        Category category
) {
}
