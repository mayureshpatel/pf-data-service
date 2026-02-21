package com.mayureshpatel.pfdataservice.dto.merchant;

import java.math.BigDecimal;

public record MerchantBreakdownDto(
        MerchantDto merchant,
        BigDecimal total
) {
}
