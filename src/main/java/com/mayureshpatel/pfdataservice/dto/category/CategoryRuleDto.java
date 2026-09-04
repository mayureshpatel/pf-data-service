package com.mayureshpatel.pfdataservice.dto.category;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CategoryRuleDto(
        Long id,
        Long userId,
        String keyword,
        Integer priority,
        CategoryDto category,
        BigDecimal minAmount,
        BigDecimal maxAmount
) {
}
