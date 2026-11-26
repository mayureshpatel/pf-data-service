package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One description link a link request created or moved, with where it pointed before, so an undo
 * can put it back.
 *
 * @param normalizedDescription the link's lookup key (the folded description)
 * @param previousMerchantId    the merchant it was linked to before, or {@code null} when it was
 *                              unlinked
 */
public record DescriptionLinkChangeDto(
        @NotBlank(message = "A normalized description is required.")
        @Size(max = 255, message = "Description must be less than 255 characters.")
        String normalizedDescription,
        Long previousMerchantId
) {
}
