package com.mayureshpatel.pfdataservice.domain.budget;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.mapper.BudgetDtoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Budget domain object tests")
class BudgetTest {

    @Test
    @DisplayName("toDto — should map all fields correctly")
    void toDto_mapsAllFields() {
        // Arrange
        User user = new User();
        user.setId(1L);

        Category category = new Category();
        category.setId(5L);
        category.setName("Entertainment");

        Budget budget = Budget.builder()
                .id(10L)
                .user(user)
                .category(category)
                .amount(new BigDecimal("200.00"))
                .month(3)
                .year(2026)
                .build();

        // Act
        BudgetDto dto = BudgetDtoMapper.toDto(budget);

        // Assert
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.category().id()).isEqualTo(5L);
        assertThat(dto.category().name()).isEqualTo("Entertainment");
        assertThat(dto.amount()).isEqualByComparingTo("200.00");
        assertThat(dto.month()).isEqualTo(3);
        assertThat(dto.year()).isEqualTo(2026);
    }
}
