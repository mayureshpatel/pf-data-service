package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Request to merge one merchant into another: {@code mergedAwayMerchantId}'s transactions and
 * recurring transactions are reassigned to {@code survivingMerchantId}, and the merged-away
 * record is then deleted.
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class MerchantMergeRequest {

    @NotNull(message = "Surviving merchant ID cannot be null.")
    @Positive(message = "Surviving merchant ID must be a positive number.")
    private final Long survivingMerchantId;

    @NotNull(message = "Merged-away merchant ID cannot be null.")
    @Positive(message = "Merged-away merchant ID must be a positive number.")
    private final Long mergedAwayMerchantId;

    /**
     * Default constructor.
     */
    public MerchantMergeRequest() {
        this.survivingMerchantId = null;
        this.mergedAwayMerchantId = null;
    }

    /**
     * All-args constructor.
     *
     * @param survivingMerchantId  the merchant that remains after the merge
     * @param mergedAwayMerchantId the merchant being merged away and deleted
     */
    public MerchantMergeRequest(Long survivingMerchantId, Long mergedAwayMerchantId) {
        this.survivingMerchantId = survivingMerchantId;
        this.mergedAwayMerchantId = mergedAwayMerchantId;
    }
}
