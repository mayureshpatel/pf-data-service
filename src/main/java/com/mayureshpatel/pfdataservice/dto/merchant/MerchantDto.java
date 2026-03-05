package com.mayureshpatel.pfdataservice.dto.merchant;

import lombok.Builder;

@Builder
public record MerchantDto(
        Long id,
        Long userId,
        String originalName,
        String cleanName
) {
}
