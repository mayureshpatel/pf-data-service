package com.mayureshpatel.pfdataservice.domain.currency;

import com.mayureshpatel.pfdataservice.domain.CreatedAtAudit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

/**
 * ISO 4217 currency codes with display metadata.
 * Validates currency_code in accounts table.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Currency {

    private String code;
    private String name;
    private String symbol;
    private boolean active = true;

    @ToString.Exclude
    private CreatedAtAudit audit;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Currency currency = (Currency) o;
        return code != null && code.equals(currency.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
