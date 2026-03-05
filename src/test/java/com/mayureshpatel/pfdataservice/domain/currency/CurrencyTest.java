package com.mayureshpatel.pfdataservice.domain.currency;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Currency Domain Object Tests")
class CurrencyTest {

    @Test
    @DisplayName("Builder should correctly populate all fields")
    void builder_shouldPopulateFields() {
        TableAudit audit = TableAudit.insertAudit(null);
        Currency currency = Currency.builder()
                .code("USD")
                .name("US Dollar")
                .symbol("$")
                .active(true)
                .audit(audit)
                .build();

        assertEquals("USD", currency.getCode());
        assertEquals("US Dollar", currency.getName());
        assertEquals("$", currency.getSymbol());
        assertTrue(currency.isActive());
        assertEquals(audit, currency.getAudit());
    }

    @Test
    @DisplayName("toBuilder should create a mutable copy")
    void toBuilder_shouldCreateMutableCopy() {
        Currency original = Currency.builder()
                .code("USD")
                .active(true)
                .build();

        Currency modified = original.toBuilder()
                .active(false)
                .build();

        assertNotSame(original, modified);
        assertFalse(modified.isActive());
        assertTrue(original.isActive());
    }

    @Test
    @DisplayName("Equality should be based on code")
    void equality_shouldBeBasedOnCode() {
        Currency c1 = Currency.builder().code("USD").name("Dollar").build();
        Currency c2 = Currency.builder().code("USD").name("Something Else").build();
        Currency c3 = Currency.builder().code("EUR").build();

        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertEquals(c1.hashCode(), c2.hashCode());
    }
}
