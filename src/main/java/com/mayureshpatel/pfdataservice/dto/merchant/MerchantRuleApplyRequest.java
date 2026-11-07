package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The PF-921 request to create a merchant rule and apply it, in one step, to the transactions the
 * user chose in the ledger.
 *
 * @param rule           the rule to create, validated exactly as when creating a rule on its own
 * @param transactionIds the transactions to give the rule's merchant, overwriting any merchant they
 *                       have; at least one and at most {@value #MAX_TRANSACTIONS}
 */
public record MerchantRuleApplyRequest(
        @NotNull @Valid MerchantRuleCreateRequest rule,
        @NotEmpty @Size(max = MerchantRuleApplyRequest.MAX_TRANSACTIONS) List<Long> transactionIds
) {

    /** The most transactions one call may change. */
    public static final int MAX_TRANSACTIONS = 2000;
}
