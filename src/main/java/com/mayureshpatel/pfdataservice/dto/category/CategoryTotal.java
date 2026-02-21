package com.mayureshpatel.pfdataservice.dto.category;

import java.math.BigDecimal;

public record CategoryTotal(
        CategoryDto category,
        BigDecimal total
) {
}
