package com.mayureshpatel.pfdataservice.dto.rule;

import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyResultDto;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The PF-922 request to undo a rule the ledger just created and applied: the result(s) the
 * create-and-apply call(s) returned, sent back as they were. Either part may be absent, and both
 * are present when one save created a category rule and a merchant rule together.
 *
 * @param category the category create-and-apply result to undo, null if no category rule was made
 * @param merchant the merchant create-and-apply result to undo, null if no merchant rule was made
 */
public record RuleApplicationUndoRequest(
        CategoryRuleApplyResultDto category,
        MerchantRuleApplyResultDto merchant
) {

    /** An undo must say what to undo: at least one of the two parts. */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Provide a category result, a merchant result, or both.")
    public boolean isSomethingToUndo() {
        return category != null || merchant != null;
    }
}
