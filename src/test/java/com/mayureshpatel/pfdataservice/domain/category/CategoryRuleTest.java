package com.mayureshpatel.pfdataservice.domain.category;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryRule Domain Object Tests")
class CategoryRuleTest {

    @Test
    @DisplayName("Builder should correctly populate all fields")
    void builder_shouldPopulateFields() {
        TableAudit audit = TableAudit.insertAudit(null);
        User user = User.builder().id(100L).build();
        Category category = Category.builder().id(50L).build();

        CategoryRule rule = CategoryRule.builder()
                .id(1L)
                .user(user)
                .keywords(List.of("AMZN"))
                .matchType(MatchType.OR)
                .priority(10)
                .category(category)
                .minAmount(new BigDecimal("5.00"))
                .maxAmount(new BigDecimal("100.00"))
                .audit(audit)
                .build();

        assertEquals(1L, rule.getId());
        assertEquals(user, rule.getUser());
        assertEquals(List.of("AMZN"), rule.getKeywords());
        assertEquals(MatchType.OR, rule.getMatchType());
        assertEquals(10, rule.getPriority());
        assertEquals(category, rule.getCategory());
        assertEquals(new BigDecimal("5.00"), rule.getMinAmount());
        assertEquals(new BigDecimal("100.00"), rule.getMaxAmount());
        assertEquals(audit, rule.getAudit());
    }

    @Test
    @DisplayName("PF-315: builder should correctly populate a multi-keyword AND rule")
    void builder_shouldPopulateMultiKeywordAndRule() {
        CategoryRule rule = CategoryRule.builder()
                .id(1L)
                .keywords(List.of("AMZN", "MKTP"))
                .matchType(MatchType.AND)
                .build();

        assertEquals(List.of("AMZN", "MKTP"), rule.getKeywords());
        assertEquals(MatchType.AND, rule.getMatchType());
    }

    @Test
    @DisplayName("toBuilder should create a mutable copy")
    void toBuilder_shouldCreateMutableCopy() {
        CategoryRule original = CategoryRule.builder()
                .id(1L)
                .keywords(List.of("OLD"))
                .build();

        CategoryRule modified = original.toBuilder()
                .keywords(List.of("NEW"))
                .build();

        assertNotSame(original, modified);
        assertEquals(List.of("NEW"), modified.getKeywords());
        assertEquals(List.of("OLD"), original.getKeywords());
    }

    @Test
    @DisplayName("Equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        CategoryRule r1 = CategoryRule.builder().id(1L).keywords(List.of("A")).build();
        CategoryRule r2 = CategoryRule.builder().id(1L).keywords(List.of("B")).build();
        CategoryRule r3 = CategoryRule.builder().id(2L).build();

        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
