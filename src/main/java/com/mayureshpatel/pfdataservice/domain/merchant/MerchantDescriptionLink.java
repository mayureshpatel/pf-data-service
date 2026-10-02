package com.mayureshpatel.pfdataservice.domain.merchant;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A many-to-one link from a specific raw transaction description (and its
 * {@code normalizedDescription} form) to a canonical {@link Merchant}. This is the mechanism that
 * turns several visually-different raw descriptions from the same real-world merchant (different
 * POS terminal IDs, store numbers, etc.) into one consistent merchant identity.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MerchantDescriptionLink {

    @EqualsAndHashCode.Include
    private Long id;
    private Long userId;
    private Long merchantId;
    private String description;
    private String normalizedDescription;

    @ToString.Exclude
    private TableAudit audit;
}
