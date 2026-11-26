package com.mayureshpatel.pfdataservice.dto.merchant;

import java.util.List;

/**
 * The merchants suggested for one description group name (PF-982), best fit first.
 *
 * @param name      the group name as it was sent
 * @param merchants the user's merchants whose {@code MerchantNameSimilarity} score with the name
 *                  reaches the service's minimum, best score first then by name; empty when none
 *                  fits. Each
 *                  merchant's {@code linkCount} is not filled in here and reads 0.
 */
public record MerchantSuggestionDto(
        String name,
        List<MerchantDto> merchants
) {
}
