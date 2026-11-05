package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleDto;

/** Converts the {@link MerchantRule} domain object into its API-facing {@link MerchantRuleDto}. */
public final class MerchantRuleDtoMapper {

    private MerchantRuleDtoMapper() {
    }

    /**
     * @param rule the domain merchant rule to convert, or {@code null}
     * @return the equivalent DTO, or {@code null} if {@code rule} was {@code null}
     */
    public static MerchantRuleDto toDto(MerchantRule rule) {
        if (rule == null) return null;
        return new MerchantRuleDto(
                rule.getId(),
                rule.getUser() != null ? rule.getUser().getId() : null,
                rule.getKeywords(),
                rule.getMatchType(),
                rule.getPriority(),
                MerchantDtoMapper.toDto(rule.getMerchant()),
                rule.getMinAmount(),
                rule.getMaxAmount()
        );
    }
}
