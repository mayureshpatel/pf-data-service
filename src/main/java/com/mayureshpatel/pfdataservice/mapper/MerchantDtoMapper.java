package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;

/** Converts the {@link Merchant} domain object into its API-facing {@link MerchantDto}. */
public final class MerchantDtoMapper {

    private MerchantDtoMapper() {
    }

    /**
     * @param merchant the domain merchant to convert, or {@code null}
     * @return the equivalent DTO, or {@code null} if {@code merchant} was {@code null}
     */
    public static MerchantDto toDto(Merchant merchant) {
        if (merchant == null) return null;
        return new MerchantDto(
                merchant.getId(),
                merchant.getUserId(),
                merchant.getName(),
                merchant.getCity(),
                merchant.getState(),
                merchant.getPostalCode(),
                merchant.getCountry()
        );
    }
}
