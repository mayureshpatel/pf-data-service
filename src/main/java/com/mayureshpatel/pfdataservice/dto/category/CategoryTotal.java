package com.mayureshpatel.pfdataservice.dto.category;

import java.math.BigDecimal;

public record CategoryTotal(
        String categoryName,
        BigDecimal total
) {
}
