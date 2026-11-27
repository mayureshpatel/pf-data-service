package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The descriptions about to be linked to the merchant in the path, checked for look-alikes first
 * (PF-983, {@code POST /api/v1/merchants/{id}/description-links/check}). The cap is higher than
 * the bulk link's 500 because the picker checks a whole selection before linking it in batches.
 *
 * @param descriptions the raw descriptions, at most 5,000, each at most 255 characters
 */
public record DescriptionLinkCheckRequest(
        @NotEmpty(message = "At least one description is required.")
        @Size(max = 5000, message = "No more than 5000 descriptions can be checked at once.")
        List<@NotNull(message = "Description cannot be null.")
        @Size(max = 255, message = "Description must be less than 255 characters.") String> descriptions
) {
}
