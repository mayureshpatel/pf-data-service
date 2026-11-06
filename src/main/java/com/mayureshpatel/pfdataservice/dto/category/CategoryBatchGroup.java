package com.mayureshpatel.pfdataservice.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * One parent category of a batch create (PF-897) together with its subcategory names. The type,
 * color and icon are set once here and inherited by every subcategory; the per-field constraints
 * mirror {@link CategoryCreateRequest}. Blank subcategory entries are allowed on purpose (they are
 * ignored), so the list elements carry only a length limit.
 *
 * @param name          the parent category name
 * @param type          the category type shared by the parent and its subcategories
 * @param color         the color shared by the parent and its subcategories
 * @param icon          the icon shared by the parent and its subcategories
 * @param subcategories the subcategory names, one per entry; may be null or empty
 */
public record CategoryBatchGroup(
        @NotBlank(message = "Category name cannot be blank.")
        @Size(max = CategoryBatchRequest.MAX_NAME_LENGTH,
                message = "Category name '${validatedValue}' must be at most 50 characters.")
        String name,

        @Size(max = 20, message = "Category type must be less than 20 characters.")
        String type,

        @Size(max = 20, message = "Category color must be less than 20 characters.")
        String color,

        @Size(max = 50, message = "Category icon must be less than 50 characters.")
        String icon,

        List<@Size(max = CategoryBatchRequest.MAX_NAME_LENGTH,
                message = "Category name '${validatedValue}' must be at most 50 characters.") String> subcategories
) {
}
