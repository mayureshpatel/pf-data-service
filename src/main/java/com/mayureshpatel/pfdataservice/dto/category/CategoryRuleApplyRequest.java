package com.mayureshpatel.pfdataservice.dto.category;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The PF-920 request to create a category rule and apply it, in one step, to the transactions the
 * user chose in the ledger.
 *
 * @param rule           the rule to create, validated exactly as when creating a rule on its own
 * @param transactionIds the transactions to give the rule's category, overwriting any category they
 *                       have; at most {@value #MAX_TRANSACTIONS}. May be empty, which creates just the
 *                       rule (when one kind of a combined create has nothing ticked, PF-934)
 */
public record CategoryRuleApplyRequest(
        @NotNull @Valid CategoryRuleCreateRequest rule,
        @NotNull @Size(max = CategoryRuleApplyRequest.MAX_TRANSACTIONS) List<Long> transactionIds
) {

    /** The most transactions one call may change. */
    public static final int MAX_TRANSACTIONS = 2000;
}
