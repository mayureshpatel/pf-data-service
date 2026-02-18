package com.mayureshpatel.pfdataservice.dto.merchant;

import com.mayureshpatel.pfdataservice.domain.user.User;

public record MerchantDto(
        Long id,
        User user,
        String originalName,
        String cleanName
) {
}
