package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.util.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CategoryRuleDtoMapper unit tests")
class CategoryRuleDtoMapperTest {

    @Test
    @DisplayName("should return null when rule is null")
    void toDto_null_returnsNull() {
        assertThat(CategoryRuleDtoMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("should map all fields correctly")
    void toDto_fullRule_mapsAllFields() {
        CategoryRule rule = TestFixtures.aCategoryRule(TestFixtures.aUser(), TestFixtures.aCategory());

        CategoryRuleDto dto = CategoryRuleDtoMapper.toDto(rule);

        assertThat(dto.id()).isEqualTo(rule.getId());
        assertThat(dto.userId()).isEqualTo(rule.getUser().getId());
        assertThat(dto.keyword()).isEqualTo(rule.getKeyword());
        assertThat(dto.priority()).isEqualTo(rule.getPriority());
        assertThat(dto.category()).isNotNull();
        assertThat(dto.category().id()).isEqualTo(rule.getCategory().getId());
    }

    @Test
    @DisplayName("should handle null user and null category")
    void toDto_nullOptionalFields_mapsNulls() {
        CategoryRule rule = new CategoryRule();
        rule.setId(1L);
        rule.setKeyword("TEST");
        rule.setPriority(5);

        CategoryRuleDto dto = CategoryRuleDtoMapper.toDto(rule);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.userId()).isNull();
        assertThat(dto.keyword()).isEqualTo("TEST");
        assertThat(dto.priority()).isEqualTo(5);
        assertThat(dto.category()).isNull();
    }
}
