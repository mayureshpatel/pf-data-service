package com.mayureshpatel.pfdataservice.dto.merchant;

import lombok.Builder;

@Builder
public record MerchantDescriptionLinkDto(
        Long id,
        Long merchantId,
        String description
) {
}
