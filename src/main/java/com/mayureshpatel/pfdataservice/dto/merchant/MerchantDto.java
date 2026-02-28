package com.mayureshpatel.pfdataservice.dto.merchant;

public record MerchantDto(
        Long id,
        Long userId,
        String originalName,
        String cleanName
) {
}
