package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Explicitly links a raw description to a merchant. The merchant id comes from the URL path, not
 * this body -- {@code POST /api/v1/merchants/{id}/description-links}.
 *
 * @param description the raw description to link, e.g. a transaction's exact description text
 */
public record MerchantDescriptionLinkCreateRequest(
        @NotBlank(message = "Description cannot be blank.")
        @Size(max = 255, message = "Description must be less than 255 characters.")
        String description
) {
}
