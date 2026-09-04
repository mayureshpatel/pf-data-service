package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

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
