package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.rule.MatchableRule;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * A draft category rule to preview (PF-900): the same matching fields and per-field validation as
 * {@link CategoryRuleCreateRequest}, without a category, since the category plays no part in
 * which transactions match or which rule wins. The keyword list may be empty (the form has no
 * keyword yet, so the preview is simply empty) but is capped at {@value MatchableRule#MAX_KEYWORDS}, because
 * the candidate query ignores keywords past that and a silently partial preview would mislead.
 *
 * @param ruleId    the id of the saved rule being edited, or null for a new rule
 * @param keywords  the draft's keywords; may be empty
 * @param matchType how the keywords combine, defaults to {@code OR}
 * @param priority  the draft's priority, defaults to 0
 * @param minAmount optional lower bound on the absolute amount
 * @param maxAmount optional upper bound on the absolute amount
 */
public record RuleMatchPreviewRequest(
        Long ruleId,

        @NotNull(message = "Keywords cannot be null.")
        @Size(max = MatchableRule.MAX_KEYWORDS, message = MatchableRule.MAX_KEYWORDS_MESSAGE)
        List<@NotBlank(message = "Keyword cannot be blank.")
             @Size(max = 255, message = "Keyword cannot exceed 255 characters.") String> keywords,

        MatchType matchType,

        @PositiveOrZero(message = "Priority must be a positive number or zero.")
        Integer priority,

        @PositiveOrZero(message = "Minimum amount must be a positive number or zero.")
        BigDecimal minAmount,

        @PositiveOrZero(message = "Maximum amount must be a positive number or zero.")
        BigDecimal maxAmount
) {

}
