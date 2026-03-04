package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Lookup table for account types with metadata (icons, colors, labels).
 * Used by frontend for display configuration.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccountType {
    @EqualsAndHashCode.Include
    private String code;
    private String label;
    private String color;
    private String icon;
    private boolean asset;
    private Integer sortOrder;
    private boolean active;

    @ToString.Exclude
    private TableAudit audit;
}
