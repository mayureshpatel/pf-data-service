package com.mayureshpatel.pfdataservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BudgetDto(
    Long id,
    
    @NotNull(message = "Category ID is required")
    Long categoryId,
    
    String categoryName,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", message = "Amount must be at least 0")
    BigDecimal amount,
    
    @NotNull(message = "Month is required")
    @Min(1) @Max(12)
    Integer month,
    
    @NotNull(message = "Year is required")
    Integer year
) {}
