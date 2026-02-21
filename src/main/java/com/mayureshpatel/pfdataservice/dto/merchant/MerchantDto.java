package com.mayureshpatel.pfdataservice.dto.merchant;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.user.User;

public record MerchantDto(
        Long id,
        User user,
        String originalName,
        String cleanName
) {

    /**
     * Maps a {@link Merchant} domain object to its corresponding DTO representation.
     *
     * @param merchant The Merchant domain object to be mapped.
     * @return The {@link MerchantDto} representation of the provided Merchant.
     */
    public static MerchantDto mapToDto(Merchant merchant) {
        return new MerchantDto(
                merchant.getId(),
                merchant.getUser(),
                merchant.getOriginalName(),
                merchant.getName()
        );
    }
}
