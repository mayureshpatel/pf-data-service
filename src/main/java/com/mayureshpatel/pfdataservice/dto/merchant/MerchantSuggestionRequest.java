package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Asks for the merchants whose names best fit each of several description group names (PF-982),
 * one call for a whole page of the Unlinked tab. A body instead of query parameters because group
 * names can contain commas, which Spring would split a single query value on.
 *
 * @param names the group display names, at most 100, each at most 255 characters; a blank name
 *              gets no suggestions
 * @param limit the most merchants to suggest per name, 1 to 20; null means 5
 */
public record MerchantSuggestionRequest(
        @NotNull(message = "Names are required.")
        @Size(max = 100, message = "No more than 100 names can be matched at once.")
        List<@NotNull(message = "Name cannot be null.") @Size(max = 255, message = "Name must be less than 255 characters.") String> names,
        @Min(value = 1, message = "Limit must be at least 1.")
        @Max(value = 20, message = "Limit must be at most 20.")
        Integer limit
) {
}
