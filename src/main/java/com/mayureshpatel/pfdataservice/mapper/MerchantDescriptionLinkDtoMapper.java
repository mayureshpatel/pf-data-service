package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.MerchantDescriptionLink;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkDto;

public final class MerchantDescriptionLinkDtoMapper {

    private MerchantDescriptionLinkDtoMapper() {
    }

    public static MerchantDescriptionLinkDto toDto(MerchantDescriptionLink link) {
        if (link == null) return null;
        return new MerchantDescriptionLinkDto(
                link.getId(),
                link.getMerchantId(),
                link.getDescription()
        );
    }
}
