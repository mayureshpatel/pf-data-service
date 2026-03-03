package com.mayureshpatel.pfdataservice.dto.currency;

public record CurrencyDto(
        String code,
        String name,
        String symbol,
        boolean isActive
) {
}
