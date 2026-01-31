package com.mayureshpatel.pfdataservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CategoryRuleDto(
        Long id,
        @NotBlank(message = "Keyword is required")
        String keyword,
        @NotBlank(message = "Category name is required")
        String categoryName,
        Integer priority
) {}
