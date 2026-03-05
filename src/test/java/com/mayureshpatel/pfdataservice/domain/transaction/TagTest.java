package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tag Domain Object Tests")
class TagTest {

    @Test
    @DisplayName("Builder should correctly populate all fields")
    void builder_shouldPopulateFields() {
        TableAudit audit = TableAudit.insertAudit(null);
        Tag tag = Tag.builder()
                .id(1L)
                .userId(100L)
                .name("Business")
                .color("blue")
                .audit(audit)
                .build();

        assertEquals(1L, tag.getId());
        assertEquals(100L, tag.getUserId());
        assertEquals("Business", tag.getName());
        assertEquals("blue", tag.getColor());
        assertEquals(audit, tag.getAudit());
    }

    @Test
    @DisplayName("toBuilder should create a mutable copy")
    void toBuilder_shouldCreateMutableCopy() {
        Tag original = Tag.builder()
                .id(1L)
                .name("A")
                .build();

        Tag modified = original.toBuilder()
                .name("B")
                .build();

        assertNotSame(original, modified);
        assertEquals("B", modified.getName());
        assertEquals("A", original.getName());
    }

    @Test
    @DisplayName("Equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        Tag t1 = Tag.builder().id(1L).name("A").build();
        Tag t2 = Tag.builder().id(1L).name("B").build();
        Tag t3 = Tag.builder().id(2L).build();

        assertEquals(t1, t2);
        assertNotEquals(t1, t3);
        assertEquals(t1.hashCode(), t2.hashCode());
    }
}
