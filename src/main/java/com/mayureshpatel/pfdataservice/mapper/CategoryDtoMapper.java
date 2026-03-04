package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;

public final class CategoryDtoMapper {

    private CategoryDtoMapper() {
    }

    public static CategoryDto toDto(Category category) {
        if (category == null) return null;
        return new CategoryDto(
                category.getId(),
                category.getUserId() != null ? category.getUserId() : null,
                category.getName(),
                CategoryType.fromValue(category.getType()),
                category.getParentId() != null ? toDto(Category.builder().parentId(category.getParentId()).build()) : null,
                category.getIcon() != null ? category.getIcon() : null,
                category.getColor() != null ? category.getColor() : null
        );
    }
}
