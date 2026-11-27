package com.mayureshpatel.pfdataservice.dto.merchant;

import java.util.List;

/**
 * The look-alike check of descriptions about to be linked to a merchant (PF-983). An empty list
 * means every selected description fits the merchant, or nothing else competes for it.
 *
 * @param merchantId the merchant the descriptions would be linked to
 * @param warnings   one entry per flagged description group, most descriptions first
 */
public record DescriptionLinkCheckDto(
        Long merchantId,
        List<LookAlikeWarningDto> warnings
) {
}
