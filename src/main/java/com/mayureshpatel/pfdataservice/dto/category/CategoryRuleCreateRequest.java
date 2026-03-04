package com.mayureshpatel.pfdataservice.dto.category;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
public class CategoryRuleCreateRequest {

    @NotNull(message = "User ID cannot be null.")
    @Positive(message = "User ID must be a positive number.")
    private final Long userId;

    @NotNull(message = "Category ID cannot be null.")
    @Positive(message = "Category ID must be a positive number.")
    private final Long categoryId;

    @NotBlank(message = "Keyword cannot be blank.")
    @Size(max = 255, message = "Keyword cannot exceed 255 characters.")
    private final String keyword;

    @PositiveOrZero(message = "Priority must be a positive number or zero.")
    private final Integer priority;
}
