package com.mayureshpatel.pfdataservice.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
public class CategoryRuleUpdateRequest {

    @NotNull(message = "Rule ID cannot be null.")
    @Positive(message = "Rule ID must be a positive number.")
    private final Long id;

    @NotNull(message = "Category ID cannot be null.")
    @Positive(message = "Category ID must be a positive number.")
    private final Long categoryId;

    @NotBlank(message = "Keyword cannot be blank.")
    @Size(max = 255, message = "Keyword cannot exceed 255 characters.")
    private final String keyword;

    @PositiveOrZero(message = "Priority must be a positive number or zero.")
    private final Integer priority;
}
