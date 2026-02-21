package com.mayureshpatel.pfdataservice.dto.category;

import java.math.BigDecimal;

public record CategoryBreakdownDto(
        CategoryDto category,
        BigDecimal total
) {
}
