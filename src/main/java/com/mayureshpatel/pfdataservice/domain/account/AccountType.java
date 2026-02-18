package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lookup table for account types with metadata (icons, colors, labels).
 * Used by frontend for display configuration.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AccountType {

    private String code;
    private String label;
    private Iconography iconography;
    private Boolean isAsset;
    private Integer sortOrder;
    private Boolean isActive = true;

    private TableAudit audit;
}
