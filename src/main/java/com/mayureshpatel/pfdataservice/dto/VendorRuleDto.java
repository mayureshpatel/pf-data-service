package com.mayureshpatel.pfdataservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record VendorRuleDto(
        Long id,
        @NotBlank(message = "Keyword is required")
        String keyword,
        @NotBlank(message = "Vendor name is required")
        String vendorName,
        Integer priority
) {
}
