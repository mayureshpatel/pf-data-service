package com.mayureshpatel.pfdataservice.dto.vendor;

import java.math.BigDecimal;

public record VendorTotal(
        String vendorName,
        BigDecimal total
) {
}
