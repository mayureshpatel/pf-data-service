package com.mayureshpatel.pfdataservice.domain.budget;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Budget Domain Object Tests")
class BudgetTest {

    @Test
    @DisplayName("Builder should correctly populate all fields")
    void builder_shouldPopulateFields() {
        TableAudit audit = TableAudit.insertAudit(null);
        Budget budget = Budget.builder()
                .id(1L)
                .userId(100L)
                .categoryId(10L)
                .amount(new BigDecimal("500.00"))
                .month(3)
                .year(2026)
                .audit(audit)
                .build();

        assertEquals(1L, budget.getId());
        assertEquals(100L, budget.getUserId());
        assertEquals(10L, budget.getCategoryId());
        assertEquals(new BigDecimal("500.00"), budget.getAmount());
        assertEquals(3, budget.getMonth());
        assertEquals(2026, budget.getYear());
        assertEquals(audit, budget.getAudit());
    }

    @Test
    @DisplayName("toBuilder should create a mutable copy")
    void toBuilder_shouldCreateMutableCopy() {
        Budget original = Budget.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .build();

        Budget modified = original.toBuilder()
                .amount(new BigDecimal("200.00"))
                .build();

        assertNotSame(original, modified);
        assertEquals(new BigDecimal("200.00"), modified.getAmount());
        assertEquals(new BigDecimal("100.00"), original.getAmount());
    }

    @Test
    @DisplayName("Equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        Budget b1 = Budget.builder().id(1L).amount(BigDecimal.ONE).build();
        Budget b2 = Budget.builder().id(1L).amount(BigDecimal.TEN).build();
        Budget b3 = Budget.builder().id(2L).build();

        assertEquals(b1, b2);
        assertNotEquals(b1, b3);
        assertEquals(b1.hashCode(), b2.hashCode());
    }
}
