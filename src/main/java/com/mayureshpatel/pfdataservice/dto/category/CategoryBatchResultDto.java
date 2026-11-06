package com.mayureshpatel.pfdataservice.dto.category;

import java.util.List;

/**
 * The outcome of a batch category create (PF-897).
 *
 * @param createdCount         the total number of categories created
 * @param createdParents       how many of them are parent categories
 * @param createdSubcategories how many of them are subcategories
 * @param skipped              the names that were not created, with reasons
 */
public record CategoryBatchResultDto(
        int createdCount,
        int createdParents,
        int createdSubcategories,
        List<CategoryBatchSkippedDto> skipped
) {
}
