package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;

public record CategoryDto(
        Long id,
        User user,
        String name,
        Category parent,
        Iconography iconography
) {

    /**
     * Maps a {@link Category} domain object to its corresponding DTO representation.
     *
     * @param category The Category domain object to be mapped.
     * @return The {@link CategoryDto} representation of the provided Category.
     */
    public static CategoryDto mapToDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getUser(),
                category.getName(),
                category.getParent(),
                category.getIconography()
        );
    }
}
