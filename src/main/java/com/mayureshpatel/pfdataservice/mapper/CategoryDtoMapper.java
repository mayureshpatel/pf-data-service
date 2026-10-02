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

        boolean hasParent = category.getParentId() != null && category.getParentId() != 0;
        CategoryDto parentDto = hasParent ? toParentDto(category) : null;

        return new CategoryDto(
                category.getId(),
                category.getUserId(),
                category.getName(),
                toType(category.getType()),
                parentDto,
                category.getIcon(),
                category.getColor()
        );
    }

    /**
     * The parent category's own one-level-deep DTO (PF-809: extracted from
     * {@link #toDto(Category)}), built directly from the parent's own fields rather than a
     * recursive {@code toDto} call, since its own {@code parent} always stays {@code null}.
     */
    private static CategoryDto toParentDto(Category category) {
        return CategoryDto.builder()
                .id(category.getParentId())
                .userId(category.getParent().getUserId())
                .name(category.getParent().getName())
                .type(toType(category.getParent().getType()))
                .parent(null)
                .icon(category.getParent().getIcon())
                .color(category.getParent().getColor())
                .build();
    }

    private static CategoryType toType(String type) {
        return type != null ? CategoryType.fromValue(type) : null;
    }
}
