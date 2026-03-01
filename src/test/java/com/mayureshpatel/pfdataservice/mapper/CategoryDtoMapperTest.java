package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.util.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CategoryDtoMapper unit tests")
class CategoryDtoMapperTest {

    @Test
    @DisplayName("should return null when category is null")
    void toDto_nullCategory_returnsNull() {
        assertThat(CategoryDtoMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("should map all fields correctly for top-level category")
    void toDto_topLevelCategory_mapsAllFields() {
        User user = TestFixtures.aUser();
        Category category = TestFixtures.aCategory(user);

        CategoryDto dto = CategoryDtoMapper.toDto(category);

        assertThat(dto.id()).isEqualTo(category.getId());
        assertThat(dto.userId()).isEqualTo(category.getUser().getId());
        assertThat(dto.name()).isEqualTo(category.getName());
        assertThat(dto.categoryType()).isEqualTo(category.getType());
        assertThat(dto.parent()).isNull();
        assertThat(dto.icon()).isEqualTo(category.getIconography().getIcon());
        assertThat(dto.color()).isEqualTo(category.getIconography().getColor());
    }

    @Test
    @DisplayName("should recursively map parent category")
    void toDto_categoryWithParent_mapsParentRecursively() {
        User user = TestFixtures.aUser();
        Category parent = new Category();
        parent.setId(99L);
        parent.setUser(user);
        parent.setName("Food");
        parent.setType(CategoryType.EXPENSE);
        
        Category child = TestFixtures.aCategory(user);
        child.setParent(parent);

        CategoryDto dto = CategoryDtoMapper.toDto(child);

        assertThat(dto.parent()).isNotNull();
        assertThat(dto.parent().id()).isEqualTo(99L);
        assertThat(dto.parent().name()).isEqualTo("Food");
        assertThat(dto.parent().parent()).isNull();
    }

    @Test
    @DisplayName("should handle null optional fields (user, iconography)")
    void toDto_nullOptionalFields_mapsNulls() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Test");
        category.setType(CategoryType.INCOME);

        CategoryDto dto = CategoryDtoMapper.toDto(category);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.userId()).isNull();
        assertThat(dto.icon()).isNull();
        assertThat(dto.color()).isNull();
    }
}
