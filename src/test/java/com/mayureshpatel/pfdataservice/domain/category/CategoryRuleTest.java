package com.mayureshpatel.pfdataservice.domain.category;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
                .keyword("AMZN")
                .priority(10)
                .category(category)
                .audit(audit)
                .build();

        assertEquals(1L, rule.getId());
        assertEquals(user, rule.getUser());
        assertEquals("AMZN", rule.getKeyword());
        assertEquals(10, rule.getPriority());
        assertEquals(category, rule.getCategory());
        assertEquals(audit, rule.getAudit());
    }

    @Test
    @DisplayName("toBuilder should create a mutable copy")
    void toBuilder_shouldCreateMutableCopy() {
        CategoryRule original = CategoryRule.builder()
                .id(1L)
                .keyword("OLD")
                .build();

        CategoryRule modified = original.toBuilder()
                .keyword("NEW")
                .build();

        assertNotSame(original, modified);
        assertEquals("NEW", modified.getKeyword());
        assertEquals("OLD", original.getKeyword());
    }

    @Test
    @DisplayName("Equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        CategoryRule r1 = CategoryRule.builder().id(1L).keyword("A").build();
        CategoryRule r2 = CategoryRule.builder().id(1L).keyword("B").build();
        CategoryRule r3 = CategoryRule.builder().id(2L).build();

        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
