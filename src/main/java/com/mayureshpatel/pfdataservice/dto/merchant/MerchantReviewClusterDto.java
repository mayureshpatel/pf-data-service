package com.mayureshpatel.pfdataservice.dto.merchant;

import lombok.Builder;

import java.util.List;

/**
 * A cluster of a user's merchants that share the same fresh {@code MerchantNameNormalizer}
 * suggestion, surfaced for bulk review (PF-842). A merchant lands in a cluster when its current
 * {@code cleanName} is blank, or differs from what re-running the normalizer against its
 * {@code originalName} would produce right now -- this single condition catches both brand-new,
 * never-reviewed rows and merchants mislabeled by the normalizer's now-fixed chain-stripping bug
 * (PF-832), with no separate historical-backfill mechanism needed (resolves PF-833).
 *
 * @param suggestedCleanName the normalizer's current suggestion for this cluster's members
 * @param merchants          the flagged merchants sharing that suggestion
 */
@Builder
public record MerchantReviewClusterDto(
        String suggestedCleanName,
        List<MerchantDto> merchants
) {
}
