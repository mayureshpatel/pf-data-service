package com.mayureshpatel.pfdataservice.domain.currency;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ISO 4217 currency codes with display metadata.
 * Validates currency_code in accounts table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

    private String code;
    private String name;
    private String symbol;
    private Boolean isActive = true;

    private TableAudit audit;
}
