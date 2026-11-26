package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * What linking descriptions to a merchant did, returned by the link endpoints and sent back as is
 * to undo it (stateless, like the rule-application undo).
 *
 * @param merchantId           the merchant the descriptions were linked to
 * @param linkedCount          how many distinct descriptions are now linked to it
 * @param movedCount           how many of them were linked to another merchant before
 * @param transactionsFilled   how many existing transactions with no merchant got this merchant
 * @param linkChanges          each link created or moved, with its previous merchant
 * @param filledTransactionIds the transactions that got this merchant
 */
public record DescriptionLinkResultDto(
        @NotNull(message = "A merchant id is required.")
        Long merchantId,
        int linkedCount,
        int movedCount,
        int transactionsFilled,
        @NotNull(message = "Link changes are required.")
        @Size(max = 500, message = "No more than 500 link changes can be undone at once.")
        List<@Valid @NotNull DescriptionLinkChangeDto> linkChanges,
        @NotNull(message = "Filled transaction ids are required.")
        @Size(max = MAX_FILLED, message = "No more than 50000 transactions can be undone at once.")
        List<@NotNull Long> filledTransactionIds
) {

    /**
     * Most transactions one link request may fill, so its result always fits back through the undo
     * endpoint; a request that would fill more is rejected and rolled back.
     */
    public static final int MAX_FILLED = 50_000;
}
