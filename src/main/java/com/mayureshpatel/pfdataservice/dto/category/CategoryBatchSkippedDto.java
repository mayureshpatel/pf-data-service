package com.mayureshpatel.pfdataservice.dto.category;

/**
 * A name a batch create did not create (PF-897), and why.
 *
 * @param name       the skipped category name
 * @param parentName the parent's name for a skipped subcategory, null for a skipped parent
 * @param reason     a short human-readable reason, such as "Already exists"
 */
public record CategoryBatchSkippedDto(String name, String parentName, String reason) {
}
