package com.mayureshpatel.pfdataservice.dto.category;

import java.util.List;

/**
 * A compact reference to an existing category rule (PF-900), used by the match preview to name
 * the other rules that also match a transaction.
 *
 * @param id           the rule id
 * @param keywords     the rule's keyword set
 * @param categoryName the name of the category the rule assigns, may be null
 * @param priority     the rule's priority (higher wins)
 */
public record RuleRefDto(
        Long id,
        List<String> keywords,
        String categoryName,
        Integer priority
) {
}
