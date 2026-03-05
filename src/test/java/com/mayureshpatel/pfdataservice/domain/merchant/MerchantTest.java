package com.mayureshpatel.pfdataservice.domain.merchant;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Merchant Domain Object Tests")
class MerchantTest {

    @Test
    @DisplayName("Builder should correctly populate all fields")
    void builder_shouldPopulateFields() {
        TableAudit audit = TableAudit.insertAudit(null);
        Merchant merchant = Merchant.builder()
                .id(1L)
                .userId(100L)
                .originalName("WHOLEFDS #12345")
                .cleanName("Whole Foods")
                .audit(audit)
                .build();

        assertEquals(1L, merchant.getId());
        assertEquals(100L, merchant.getUserId());
        assertEquals("WHOLEFDS #12345", merchant.getOriginalName());
        assertEquals("Whole Foods", merchant.getCleanName());
        assertEquals(audit, merchant.getAudit());
    }

    @Test
    @DisplayName("toBuilder should create a mutable copy")
    void toBuilder_shouldCreateMutableCopy() {
        Merchant original = Merchant.builder()
                .id(1L)
                .cleanName("Merchant A")
                .build();

        Merchant modified = original.toBuilder()
                .cleanName("Merchant B")
                .build();

        assertNotSame(original, modified);
        assertEquals("Merchant B", modified.getCleanName());
        assertEquals("Merchant A", original.getCleanName());
    }

    @Test
    @DisplayName("Equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        Merchant m1 = Merchant.builder().id(1L).cleanName("A").build();
        Merchant m2 = Merchant.builder().id(1L).cleanName("B").build();
        Merchant m3 = Merchant.builder().id(2L).build();

        assertEquals(m1, m2);
        assertNotEquals(m1, m3);
        assertEquals(m1.hashCode(), m2.hashCode());
    }
}
