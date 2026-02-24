package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;

public record CategoryDto(
        Long id,
        User user,
        String name,
        CategoryType categoryType,
        CategoryDto parent,
        Iconography iconography
) {

    /**
     * Maps a {@link com.mayureshpatel.pfdataservice.domain.category.CategoryDto} domain object to its corresponding DTO representation.
     *
     * @param category The {@link com.mayureshpatel.pfdataservice.domain.category.CategoryDto} domain object to be mapped.
     * @return The {@link CategoryDto} representation of the provided {@link com.mayureshpatel.pfdataservice.domain.category.CategoryDto}.
     */
    public static CategoryDto mapToDto(com.mayureshpatel.pfdataservice.domain.category.CategoryDto category) {
        CategoryDto parent = null;
        if (category.getParent() != null) {
            parent = mapToDto(category.getParent());
        }

        return new CategoryDto(
                category.getId(),
                category.getUser(),
                category.getName(),
                category.getType(),
                parent,
                category.getIconography()
        );
    }
}
