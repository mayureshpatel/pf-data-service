package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;

public final class MerchantDtoMapper {

    private MerchantDtoMapper() {}

    public static MerchantDto toDto(Merchant merchant) {
        if (merchant == null) return null;
        return new MerchantDto(
                merchant.getId(),
                merchant.getUser() != null ? merchant.getUser().getId() : null,
                merchant.getOriginalName(),
                merchant.getCleanName()
        );
    }
}
