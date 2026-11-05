package com.mayureshpatel.pfdataservice.dto.merchant;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * A draft merchant rule to preview (PF-905): the same matching fields and per-field validation as
 * the category preview request, plus an optional target merchant that is only used to name it in
 * the output. The keyword list may be empty (the form has no keyword yet, so the preview is simply
 * empty) but is capped at {@value #MAX_KEYWORDS}, because the candidate query ignores keywords past
 * that and a silently partial preview would mislead.
 *
 * @param ruleId     the id of the saved rule being edited, or null for a new rule
 * @param keywords   the draft's keywords; may be empty
 * @param matchType  how the keywords combine, defaults to {@code OR}
 * @param priority   the draft's priority, defaults to 0
 * @param minAmount  optional lower bound on the absolute amount
 * @param maxAmount  optional upper bound on the absolute amount
 * @param merchantId optional target merchant, validated as the caller's own and used only for its name
 */
public record MerchantRuleMatchPreviewRequest(
        Long ruleId,

        @NotNull(message = "Keywords cannot be null.")
        @Size(max = MerchantRuleMatchPreviewRequest.MAX_KEYWORDS, message = "A rule can have at most 20 keywords.")
        List<@NotBlank(message = "Keyword cannot be blank.")
             @Size(max = 255, message = "Keyword cannot exceed 255 characters.") String> keywords,

        MatchType matchType,

        @PositiveOrZero(message = "Priority must be a positive number or zero.")
        Integer priority,

        @PositiveOrZero(message = "Minimum amount must be a positive number or zero.")
        BigDecimal minAmount,

        @PositiveOrZero(message = "Maximum amount must be a positive number or zero.")
        BigDecimal maxAmount,

        Long merchantId
) {

    /** The most keywords a previewed draft may carry; matches the candidate query's cap. */
    public static final int MAX_KEYWORDS = 20;
}
