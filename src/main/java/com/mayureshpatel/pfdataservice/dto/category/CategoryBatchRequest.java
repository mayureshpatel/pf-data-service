package com.mayureshpatel.pfdataservice.dto.category;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The request payload for creating many categories at once (PF-897): parent categories, each with
 * the names of its subcategories. The owner is never part of the body; it always comes from the
 * authenticated principal.
 *
 * @param groups the parent categories to create, between 1 and {@value #MAX_GROUPS}
 */
public record CategoryBatchRequest(
        @NotEmpty(message = "At least one category group is required.")
        @Size(max = MAX_GROUPS, message = "A batch can contain at most 50 category groups.")
        List<@NotNull(message = "A category group cannot be null.") @Valid CategoryBatchGroup> groups
) {

    /** The most parent groups one batch may contain. */
    public static final int MAX_GROUPS = 50;

    /** The most names (parents plus subcategories) one batch may contain. */
    public static final int MAX_NAMES = 300;

    /** The longest category name, the same limit the single create request enforces. */
    public static final int MAX_NAME_LENGTH = 50;

    /**
     * Counts every parent and subcategory name in the batch, including blank ones.
     *
     * @return the total number of names, 0 when there are no groups
     */
    @JsonIgnore
    public int totalNames() {
        if (groups == null) {
            return 0;
        }
        return groups.stream()
                .filter(group -> group != null)
                .mapToInt(group -> 1 + (group.subcategories() == null ? 0 : group.subcategories().size()))
                .sum();
    }

    /**
     * Bean-validation hook for the total name cap.
     *
     * @return true when the batch has at most {@value #MAX_NAMES} names in total
     */
    @JsonIgnore
    @AssertTrue(message = "A batch can contain at most 300 category names in total.")
    public boolean isWithinNameLimit() {
        return totalNames() <= MAX_NAMES;
    }
}
