package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The PF-921 request to create a merchant rule and apply it, in one step, to the transactions the
 * user chose in the ledger.
 *
 * @param rule           the rule to create, validated like a plain merchant rule, except that the merchant
 *                       may be a new name to create (PF-933)
 * @param transactionIds the transactions to give the rule's merchant, overwriting any merchant they
 *                       have; at most {@value #MAX_TRANSACTIONS}. May be empty, which creates just the
 *                       rule and, if named, the new merchant (PF-933)
 */
public record MerchantRuleApplyRequest(
        @NotNull @Valid MerchantRuleApplyRule rule,
        @NotNull @Size(max = MerchantRuleApplyRequest.MAX_TRANSACTIONS) List<Long> transactionIds
) {

    /** The most transactions one call may change. */
    public static final int MAX_TRANSACTIONS = 2000;
}
