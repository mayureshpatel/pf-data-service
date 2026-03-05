package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecurringTransaction Domain Object Tests")
class RecurringTransactionTest {

    @Test
    @DisplayName("Builder should correctly populate all fields including inherited ones")
    void builder_shouldPopulateFields() {
        TableAudit audit = TableAudit.insertAudit(null);
        Merchant merchant = Merchant.builder().id(20L).cleanName("Netflix").build();
        LocalDate nextDate = LocalDate.now().plusDays(30);
        
        RecurringTransaction rt = RecurringTransaction.builder()
                .id(1L)
                .userId(100L)
                .amount(new BigDecimal("15.99"))
                .merchant(merchant)
                .frequency("MONTHLY")
                .nextDate(nextDate)
                .active(true)
                .audit(audit)
                .build();

        assertEquals(1L, rt.getId());
        assertEquals(100L, rt.getUserId());
        assertEquals(new BigDecimal("15.99"), rt.getAmount());
        assertEquals(merchant, rt.getMerchant());
        assertEquals("MONTHLY", rt.getFrequency());
        assertEquals(nextDate, rt.getNextDate());
        assertTrue(rt.isActive());
        assertEquals(audit, rt.getAudit());
    }

    @Test
    @DisplayName("toBuilder should create a mutable copy")
    void toBuilder_shouldCreateMutableCopy() {
        RecurringTransaction original = RecurringTransaction.builder()
                .id(1L)
                .active(true)
                .build();

        RecurringTransaction modified = original.toBuilder()
                .active(false)
                .build();

        assertNotSame(original, modified);
        assertFalse(modified.isActive());
        assertTrue(original.isActive());
    }

    @Test
    @DisplayName("Equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        RecurringTransaction r1 = RecurringTransaction.builder().id(1L).frequency("A").build();
        RecurringTransaction r2 = RecurringTransaction.builder().id(1L).frequency("B").build();
        RecurringTransaction r3 = RecurringTransaction.builder().id(2L).build();

        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
