package com.mayureshpatel.pfdataservice.dto.merchant;

import com.mayureshpatel.pfdataservice.domain.rule.MatchableRule;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
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

/**
 * The request payload for updating an existing merchant rule, identified by {@code id}; the same
 * fields and validation as {@link MerchantRuleCreateRequest}. A null {@code matchType} defaults
 * to {@code OR} and a null {@code priority} to 0.
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class MerchantRuleUpdateRequest {

    @NotNull(message = "Rule ID cannot be null.")
    @Positive(message = "Rule ID must be a positive number.")
    private final Long id;

    @NotNull(message = "Merchant ID cannot be null.")
    @Positive(message = "Merchant ID must be a positive number.")
    private final Long merchantId;

    @NotEmpty(message = "At least one keyword is required.")
    @Size(max = MatchableRule.MAX_KEYWORDS, message = MatchableRule.MAX_KEYWORDS_MESSAGE)
    private final List<@NotBlank(message = "Keyword cannot be blank.")
                        @Size(max = 255, message = "Keyword cannot exceed 255 characters.") String> keywords;

    private final MatchType matchType;

    @PositiveOrZero(message = "Priority must be a positive number or zero.")
    private final Integer priority;

    @PositiveOrZero(message = "Minimum amount must be a positive number or zero.")
    private final BigDecimal minAmount;

    @PositiveOrZero(message = "Maximum amount must be a positive number or zero.")
    private final BigDecimal maxAmount;

    /**
     * @return true unless both bounds are set and the minimum exceeds the maximum
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Minimum amount cannot exceed maximum amount.")
    public boolean isAmountRangeConsistent() {
        return minAmount == null || maxAmount == null || minAmount.compareTo(maxAmount) <= 0;
    }
}
