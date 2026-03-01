package com.mayureshpatel.pfdataservice.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoryRuleDto(
        Long id,
        Long userId,
        
        @NotBlank(message = "Keyword is required")
        @Size(max = 100, message = "Keyword must be less than 100 characters")
        String keyword,
        
        @NotNull(message = "Priority is required")
        @PositiveOrZero(message = "Priority must be zero or positive")
        Integer priority,
        
        @NotNull(message = "Category is required")
        CategoryDto category
) {
}
