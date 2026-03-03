package com.mayureshpatel.pfdataservice.domain.currency;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * ISO 4217 currency codes with display metadata.
 * Validates currency_code in accounts table.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Currency {

    @EqualsAndHashCode.Include
    private String code;
    private String name;
    private String symbol;
    private boolean active;

    @ToString.Exclude
    private CreatedAtAudit audit;
}
