package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.MerchantDescriptionLink;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkDto;

/**
 * Converts the {@link MerchantDescriptionLink} domain object into its API-facing
 * {@link MerchantDescriptionLinkDto}.
 */
public final class MerchantDescriptionLinkDtoMapper {

    private MerchantDescriptionLinkDtoMapper() {
    }

    /**
     * @param link the domain description link to convert, or {@code null}
     * @return the equivalent DTO, or {@code null} if {@code link} was {@code null}
     */
    public static MerchantDescriptionLinkDto toDto(MerchantDescriptionLink link) {
        if (link == null) return null;
        return new MerchantDescriptionLinkDto(
                link.getId(),
                link.getMerchantId(),
                link.getDescription()
        );
    }
}
