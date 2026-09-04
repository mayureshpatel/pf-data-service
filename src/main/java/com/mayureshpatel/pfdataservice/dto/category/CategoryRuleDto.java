package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CategoryRuleDto(
        Long id,
        Long userId,
        List<String> keywords,
        MatchType matchType,
        Integer priority,
        CategoryDto category,
        BigDecimal minAmount,
        BigDecimal maxAmount
) {
}
