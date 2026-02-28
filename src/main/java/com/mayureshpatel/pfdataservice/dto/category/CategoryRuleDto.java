package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.Builder;

@Builder
public record CategoryRuleDto(
        Long id,
        Long userId,
        String keyword,
        Integer priority,
        CategoryDto category
) {

    /**
     * Maps a {@link CategoryRule} entity to a {@link CategoryRuleDto}
     *
     * @param rule the {@link CategoryRule} entity to map
     * @return the mapped {@link CategoryRuleDto}
     */
    public static CategoryRuleDto mapToDto(CategoryRule rule) {
        return CategoryRuleDto.builder()
                .id(rule.getId())
                .userId(rule.getUser().getId())
                .keyword(rule.getKeyword())
                .category(CategoryDto.mapToDto(rule.getCategory()))
                .priority(rule.getPriority())
                .build();
    }
}
