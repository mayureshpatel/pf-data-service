package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountType Domain Object Tests")
class AccountTypeTest {

    @Test
    @DisplayName("Builder should correctly populate all fields")
    void builder_shouldPopulateFields() {
        TableAudit audit = TableAudit.insertAudit(null);
        AccountType type = AccountType.builder()
                .code("CHECKING")
                .label("Checking Account")
                .color("blue")
                .icon("pi-wallet")
                .asset(true)
                .sortOrder(1)
                .active(true)
                .audit(audit)
                .build();

        assertEquals("CHECKING", type.getCode());
        assertEquals("Checking Account", type.getLabel());
        assertEquals("blue", type.getColor());
        assertEquals("pi-wallet", type.getIcon());
        assertTrue(type.isAsset());
        assertEquals(1, type.getSortOrder());
        assertTrue(type.isActive());
        assertEquals(audit, type.getAudit());
    }

    @Test
    @DisplayName("toBuilder should create a mutable copy")
    void toBuilder_shouldCreateMutableCopy() {
        AccountType original = AccountType.builder()
                .code("CHECKING")
                .active(true)
                .build();

        AccountType modified = original.toBuilder()
                .active(false)
                .build();

        assertNotSame(original, modified);
        assertFalse(modified.isActive());
        assertTrue(original.isActive());
    }

    @Test
    @DisplayName("Equality should be based on code")
    void equality_shouldBeBasedOnCode() {
        AccountType t1 = AccountType.builder().code("CHECKING").label("A").build();
        AccountType t2 = AccountType.builder().code("CHECKING").label("B").build();
        AccountType t3 = AccountType.builder().code("SAVINGS").build();

        assertEquals(t1, t2);
        assertNotEquals(t1, t3);
        assertEquals(t1.hashCode(), t2.hashCode());
    }
}
