package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;

public final class CategoryDtoMapper {

    private CategoryDtoMapper() {}

    public static CategoryDto toDto(Category category) {
        if (category == null) return null;
        return new CategoryDto(
                category.getId(),
                category.getUser() != null ? category.getUser().getId() : null,
                category.getName(),
                category.getType(),
                category.getParent() != null ? toDto(category.getParent()) : null,
                category.getIconography() != null ? category.getIconography().getIcon() : null,
                category.getIconography() != null ? category.getIconography().getColor() : null
        );
    }
}
