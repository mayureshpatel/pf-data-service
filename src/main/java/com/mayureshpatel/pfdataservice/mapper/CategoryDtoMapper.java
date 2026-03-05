package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;

public final class CategoryDtoMapper {

    private CategoryDtoMapper() {
    }

    public static CategoryDto toDto(Category category) {
        if (category == null) return null;
        
        CategoryDto parentDto = null;
        if (category.getParentId() != null && category.getParentId() != 0) {
            parentDto = CategoryDto.builder()
                    .id(category.getParentId())
                    .build();
        }

        return new CategoryDto(
                category.getId(),
                category.getUserId() != null ? category.getUserId() : null,
                category.getName(),
                category.getType() != null ? CategoryType.fromValue(category.getType()) : null,
                parentDto,
                category.getIcon() != null ? category.getIcon() : null,
                category.getColor() != null ? category.getColor() : null
        );
    }
}
