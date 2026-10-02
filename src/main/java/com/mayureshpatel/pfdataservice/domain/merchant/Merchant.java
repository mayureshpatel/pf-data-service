package com.mayureshpatel.pfdataservice.domain.merchant;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A normalized merchant identity (e.g. "Starbucks"), decoupled from the raw, inconsistently
 * formatted description text a bank's CSV export actually contains (e.g.
 * "SQ *STARBUCKS #4471"). {@link MerchantDescriptionLink} is what maps a specific raw description
 * to one of these; this class only holds the canonical identity itself.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Merchant {

    @EqualsAndHashCode.Include
    private Long id;
    private Long userId;
    private String name;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    @ToString.Exclude
    private TableAudit audit;
}
