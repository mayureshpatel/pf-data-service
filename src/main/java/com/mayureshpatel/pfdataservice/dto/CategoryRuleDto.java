package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CategoryRuleDto(
        Long id,
        @NotBlank(message = "Keyword is required")
        String keyword,
        @NotBlank(message = "Category is required")
        Category category,
        Integer priority
) {}
