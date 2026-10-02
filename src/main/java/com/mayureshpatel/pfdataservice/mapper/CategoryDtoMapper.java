package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;

/** Converts the {@link Category} domain object into its API-facing {@link CategoryDto}. */
public final class CategoryDtoMapper {

    private CategoryDtoMapper() {
    }

    /**
     * Also hand-builds the parent category's own DTO one level deep when {@code category} is a
     * subcategory, but stops there -- the parent DTO's own {@code parent} is always {@code null},
     * since this codebase's category hierarchy only ever goes one level deep in practice.
     *
     * @param category the domain category to convert, or {@code null}
     * @return the equivalent DTO, or {@code null} if {@code category} was {@code null}
     */
    public static CategoryDto toDto(Category category) {
        if (category == null) return null;
        
        CategoryDto parentDto = null;
        if (category.getParentId() != null && category.getParentId() != 0) {
            parentDto = CategoryDto.builder()
                    .id(category.getParentId())
                    .userId(category.getParent().getUserId() != null ? category.getParent().getUserId() : null)
                    .name(category.getParent().getName())
                    .type(category.getParent().getType() != null ? CategoryType.fromValue(category.getParent().getType()) : null)
                    .parent(null)
                    .icon(category.getParent().getIcon() != null ? category.getParent().getIcon() : null)
                    .color(category.getParent().getColor() != null ? category.getParent().getColor() : null)
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
