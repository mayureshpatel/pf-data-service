//package com.mayureshpatel.pfdataservice.mapper;
//
//import com.mayureshpatel.pfdataservice.domain.budget.Budget;
//import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DisplayName("BudgetDtoMapper unit tests")
//class BudgetDtoMapperTest {
//
//    @Test
//    @DisplayName("should return null when budget is null")
//    void toDto_nullBudget_returnsNull() {
//        assertThat(BudgetDtoMapper.toDto(null)).isNull();
//    }
//
//    @Test
//    @DisplayName("should map all fields correctly")
//    void toDto_fullBudget_mapsAllFields() {
//        Budget budget = TestFixtures.aBudget();
//
//        BudgetDto dto = BudgetDtoMapper.toDto(budget);
//
//        assertThat(dto.id()).isEqualTo(budget.getId());
//        assertThat(dto.userId()).isEqualTo(budget.getUser().getId());
//        assertThat(dto.amount()).isEqualByComparingTo(budget.getAmount());
//        assertThat(dto.month()).isEqualTo(budget.getMonth());
//        assertThat(dto.year()).isEqualTo(budget.getYear());
//        assertThat(dto.category()).isNotNull();
//        assertThat(dto.category().id()).isEqualTo(budget.getCategory().getId());
//    }
//
//    @Test
//    @DisplayName("should handle null user and null category")
//    void toDto_nullOptionalFields_mapsNulls() {
//        Budget budget = Budget.builder()
//                .id(1L)
//                .amount(new java.math.BigDecimal("100.00"))
//                .month(6)
//                .year(2026)
//                .build();
//
//        BudgetDto dto = BudgetDtoMapper.toDto(budget);
//
//        assertThat(dto.id()).isEqualTo(1L);
//        assertThat(dto.userId()).isNull();
//        assertThat(dto.category()).isNull();
//        assertThat(dto.amount()).isEqualByComparingTo("100.00");
//    }
//}
