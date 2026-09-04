package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

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

    @NotEmpty(message = "At least one keyword is required.")
    private final List<@NotBlank(message = "Keyword cannot be blank.")
                        @Size(max = 255, message = "Keyword cannot exceed 255 characters.") String> keywords;

    private final MatchType matchType;

    @PositiveOrZero(message = "Priority must be a positive number or zero.")
    private final Integer priority;

    @PositiveOrZero(message = "Minimum amount must be a positive number or zero.")
    private final BigDecimal minAmount;

    @PositiveOrZero(message = "Maximum amount must be a positive number or zero.")
    private final BigDecimal maxAmount;
}
