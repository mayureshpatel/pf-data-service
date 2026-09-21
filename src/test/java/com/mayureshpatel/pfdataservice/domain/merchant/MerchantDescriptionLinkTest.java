package com.mayureshpatel.pfdataservice.domain.merchant;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MerchantDescriptionLink Domain Object Tests")
class MerchantDescriptionLinkTest {

    @Test
    @DisplayName("Builder should correctly populate all fields")
    void builder_shouldPopulateFields() {
        TableAudit audit = TableAudit.insertAudit(null);
        MerchantDescriptionLink link = MerchantDescriptionLink.builder()
                .id(1L)
                .userId(100L)
                .merchantId(7L)
                .description("STARBUCKS #1")
                .normalizedDescription("starbucks #1")
                .audit(audit)
                .build();

        assertEquals(1L, link.getId());
        assertEquals(100L, link.getUserId());
        assertEquals(7L, link.getMerchantId());
        assertEquals("STARBUCKS #1", link.getDescription());
        assertEquals("starbucks #1", link.getNormalizedDescription());
        assertEquals(audit, link.getAudit());
    }

    @Test
    @DisplayName("toBuilder should create a mutable copy")
    void toBuilder_shouldCreateMutableCopy() {
        MerchantDescriptionLink original = MerchantDescriptionLink.builder()
                .id(1L)
                .description("Description A")
                .build();

        MerchantDescriptionLink modified = original.toBuilder()
                .description("Description B")
                .build();

        assertNotSame(original, modified);
        assertEquals("Description B", modified.getDescription());
        assertEquals("Description A", original.getDescription());
    }

    @Test
    @DisplayName("Equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        MerchantDescriptionLink l1 = MerchantDescriptionLink.builder().id(1L).description("A").build();
        MerchantDescriptionLink l2 = MerchantDescriptionLink.builder().id(1L).description("B").build();
        MerchantDescriptionLink l3 = MerchantDescriptionLink.builder().id(2L).build();

        assertEquals(l1, l2);
        assertNotEquals(l1, l3);
        assertEquals(l1.hashCode(), l2.hashCode());
    }
}
