package com.mayureshpatel.pfdataservice.dto.transaction.recurring;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dismisses a detected recurring pattern.
 *
 * @param patternKey the pattern's key as reported by the suggestion (folded again server-side)
 * @param name       the detected name, kept so the dismissed list is readable
 */
public record DismissPatternRequest(
        @NotBlank(message = "Pattern key cannot be blank.")
        @Size(max = 255, message = "Pattern key must be less than 255 characters.")
        String patternKey,
        @NotBlank(message = "Name cannot be blank.")
        @Size(max = 255, message = "Name must be less than 255 characters.")
        String name
) {
}
