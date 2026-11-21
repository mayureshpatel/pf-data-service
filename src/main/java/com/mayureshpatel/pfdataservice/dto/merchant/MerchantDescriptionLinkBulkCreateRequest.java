package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Explicitly links many raw descriptions to one merchant in a single call. The merchant id comes
 * from the URL path -- {@code POST /api/v1/merchants/{id}/description-links/bulk}. Exists so the
 * description picker can link a whole selection at once instead of one request per description.
 *
 * @param descriptions the raw descriptions to link, each non-blank and at most 255 characters;
 *                     at most 500 per call
 */
public record MerchantDescriptionLinkBulkCreateRequest(
        @NotEmpty(message = "At least one description is required.")
        @Size(max = 500, message = "No more than 500 descriptions can be linked at once.")
        List<@NotBlank(message = "Description cannot be blank.")
        @Size(max = 255, message = "Description must be less than 255 characters.") String> descriptions
) {
}
