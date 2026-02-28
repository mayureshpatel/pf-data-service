package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;

public final class CategoryRuleDtoMapper {

    private CategoryRuleDtoMapper() {}

    public static CategoryRuleDto toDto(CategoryRule rule) {
        if (rule == null) return null;
        return new CategoryRuleDto(
                rule.getId(),
                rule.getUser() != null ? rule.getUser().getId() : null,
                rule.getKeyword(),
                rule.getPriority(),
                CategoryDtoMapper.toDto(rule.getCategory())
        );
    }
}
