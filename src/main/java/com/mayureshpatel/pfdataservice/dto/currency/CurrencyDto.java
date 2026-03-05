package com.mayureshpatel.pfdataservice.dto.currency;

import lombok.Builder;

@Builder(toBuilder = true)
public record CurrencyDto(
        String code,
        String name,
        String symbol,
        boolean isActive
) {
}
