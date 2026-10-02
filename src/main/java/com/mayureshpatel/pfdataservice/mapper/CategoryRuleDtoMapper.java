package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;

/** Converts the {@link CategoryRule} domain object into its API-facing {@link CategoryRuleDto}. */
public final class CategoryRuleDtoMapper {

    private CategoryRuleDtoMapper() {
    }

    /**
     * @param rule the domain category rule to convert, or {@code null}
     * @return the equivalent DTO, or {@code null} if {@code rule} was {@code null}
     */
    public static CategoryRuleDto toDto(CategoryRule rule) {
        if (rule == null) return null;
        return new CategoryRuleDto(
                rule.getId(),
                rule.getUser() != null ? rule.getUser().getId() : null,
                rule.getKeywords(),
                rule.getMatchType(),
                rule.getPriority(),
                CategoryDtoMapper.toDto(rule.getCategory()),
                rule.getMinAmount(),
                rule.getMaxAmount()
        );
    }
}
