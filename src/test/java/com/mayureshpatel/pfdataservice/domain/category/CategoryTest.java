//package com.mayureshpatel.pfdataservice.domain.category;
//
//import com.mayureshpatel.pfdataservice.domain.Iconography;
//import com.mayureshpatel.pfdataservice.domain.user.User;
//import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
//import com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DisplayName("Category domain object tests")
//class CategoryTest {
//
//    @Test
//    @DisplayName("toDto — should map all fields correctly")
//    void toDto_mapsAllFields() {
//        // Arrange
//        User user = new User();
//        user.setId(1L);
//
//        Category parent = new Category();
//        parent.setId(5L);
//        parent.setName("Parent");
//
//        Category category = new Category();
//        category.setId(10L);
//        category.setUser(user);
//        category.setName("Food");
//        category.setType(CategoryType.EXPENSE);
//        category.setIconography(new Iconography("fastfood", "#FF0000"));
//        category.setParent(parent);
//
//        // Act
//        CategoryDto dto = CategoryDtoMapper.toDto(category);
//
//        // Assert
//        assertThat(dto.id()).isEqualTo(10L);
//        assertThat(dto.name()).isEqualTo("Food");
//        assertThat(dto.categoryType()).isEqualTo(CategoryType.EXPENSE);
//        assertThat(dto.icon()).isEqualTo("fastfood");
//        assertThat(dto.color()).isEqualTo("#FF0000");
//        assertThat(dto.parent()).isNotNull();
//        assertThat(dto.parent().id()).isEqualTo(5L);
//        assertThat(dto.parent().name()).isEqualTo("Parent");
//    }
//}
