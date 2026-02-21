package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;

public record CategoryDto(
        Long id,
        User user,
        String name,
        CategoryType categoryType,
        Category parent,
        Iconography iconography
) {

    /**
     * Maps a {@link Category} domain object to its corresponding DTO representation.
     *
     * @param category The {@link Category} domain object to be mapped.
     * @return The {@link CategoryDto} representation of the provided {@link Category}.
     */
    public static CategoryDto mapToDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getUser(),
                category.getName(),
                category.getType(),
                category.getParent(),
                category.getIconography()
        );
    }
}
