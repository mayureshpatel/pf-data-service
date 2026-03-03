package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.TimestampAudit;
import lombok.*;

/**
 * Lookup table for account types with metadata (icons, colors, labels).
 * Used by frontend for display configuration.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class AccountType {
    @EqualsAndHashCode.Include
    private String code;
    private String label;
    @ToString.Exclude
    private Iconography iconography;
    private boolean asset;
    private Integer sortOrder;
    private boolean active;

    @ToString.Exclude
    private TimestampAudit audit;
}
