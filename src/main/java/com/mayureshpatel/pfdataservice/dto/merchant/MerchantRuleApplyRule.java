package com.mayureshpatel.pfdataservice.dto.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.rule.MatchableRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * The merchant rule a create-and-apply call (PF-921) creates. It is a merchant rule's usual fields,
 * except the merchant may be named instead of chosen (PF-933): either an existing {@code merchantId}
 * or a {@code newMerchantName} to create, never both and never neither. A name that matches one of
 * the user's merchants ignoring case reuses that merchant instead of creating a duplicate.
 *
 * @param merchantId      the existing merchant the rule assigns, or null when {@code newMerchantName} is set
 * @param newMerchantName the name of a merchant to create (in the same transaction as the rule), or null
 * @param keywords        the rule's keywords
 * @param matchType       how the keywords combine, {@code OR} when null
 * @param priority        the rule's priority, 0 when null
 * @param minAmount       optional lower bound of the amount range
 * @param maxAmount       optional upper bound of the amount range
 */
public record MerchantRuleApplyRule(
        @Positive(message = "Merchant ID must be a positive number.") Long merchantId,
        @Size(max = 255, message = "Merchant name must be less than 255 characters.") String newMerchantName,
        @NotEmpty(message = "At least one keyword is required.")
        @Size(max = MatchableRule.MAX_KEYWORDS, message = MatchableRule.MAX_KEYWORDS_MESSAGE)
        List<@NotBlank(message = "Keyword cannot be blank.")
        @Size(max = 255, message = "Keyword cannot exceed 255 characters.") String> keywords,
        MatchType matchType,
        @PositiveOrZero(message = "Priority must be a positive number or zero.") Integer priority,
        @PositiveOrZero(message = "Minimum amount must be a positive number or zero.") BigDecimal minAmount,
        @PositiveOrZero(message = "Maximum amount must be a positive number or zero.") BigDecimal maxAmount
) {

    /** Exactly one of an existing merchant and a new merchant name must be given. */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Provide either a merchant or a new merchant name, not both and not neither.")
    public boolean isMerchantChosen() {
        boolean hasName = newMerchantName != null && !newMerchantName.isBlank();
        return (merchantId != null) != hasName;
    }

    /** Minimum amount cannot exceed maximum amount. */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Minimum amount cannot exceed maximum amount.")
    public boolean isAmountRangeConsistent() {
        return minAmount == null || maxAmount == null || minAmount.compareTo(maxAmount) <= 0;
    }

    /**
     * The same rule as a plain create request for the given merchant.
     *
     * @param resolvedMerchantId the merchant the rule assigns, once a new one has been created or found
     * @return the equivalent {@link MerchantRuleCreateRequest}
     */
    public MerchantRuleCreateRequest toCreateRequest(Long resolvedMerchantId) {
        return MerchantRuleCreateRequest.builder()
                .merchantId(resolvedMerchantId)
                .keywords(keywords)
                .matchType(matchType)
                .priority(priority)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .build();
    }
}
