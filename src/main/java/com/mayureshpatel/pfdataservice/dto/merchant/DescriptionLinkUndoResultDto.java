package com.mayureshpatel.pfdataservice.dto.merchant;

import java.util.List;

/**
 * What undoing a description link request put back, and what it left because it changed since.
 *
 * @param linksRestored         links returned to their previous merchant, or removed when they
 *                              were new or that merchant no longer exists
 * @param linksSkipped          links left alone because they now point at a different merchant
 *                              or were already removed
 * @param transactionsRestored  transactions whose merchant was cleared again
 * @param skippedTransactionIds transactions left alone because their merchant was changed since
 *                              (or they were deleted)
 */
public record DescriptionLinkUndoResultDto(
        int linksRestored,
        int linksSkipped,
        int transactionsRestored,
        List<Long> skippedTransactionIds
) {
}
