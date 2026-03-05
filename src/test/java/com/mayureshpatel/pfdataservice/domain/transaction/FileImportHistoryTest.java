package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileImportHistory Domain Object Tests")
class FileImportHistoryTest {

    @Test
    @DisplayName("Builder should correctly populate all fields")
    void builder_shouldPopulateFields() {
        TableAudit audit = TableAudit.insertAudit(null);
        FileImportHistory history = FileImportHistory.builder()
                .id(1L)
                .accountId(10L)
                .fileName("transactions.csv")
                .fileHash("hash123")
                .transactionCount(50)
                .audit(audit)
                .build();

        assertEquals(1L, history.getId());
        assertEquals(10L, history.getAccountId());
        assertEquals("transactions.csv", history.getFileName());
        assertEquals("hash123", history.getFileHash());
        assertEquals(50, history.getTransactionCount());
        assertEquals(audit, history.getAudit());
    }

    @Test
    @DisplayName("toBuilder should create a mutable copy")
    void toBuilder_shouldCreateMutableCopy() {
        FileImportHistory original = FileImportHistory.builder()
                .id(1L)
                .transactionCount(10)
                .build();

        FileImportHistory modified = original.toBuilder()
                .transactionCount(20)
                .build();

        assertNotSame(original, modified);
        assertEquals(20, modified.getTransactionCount());
        assertEquals(10, original.getTransactionCount());
    }

    @Test
    @DisplayName("Equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        FileImportHistory h1 = FileImportHistory.builder().id(1L).fileName("A").build();
        FileImportHistory h2 = FileImportHistory.builder().id(1L).fileName("B").build();
        FileImportHistory h3 = FileImportHistory.builder().id(2L).build();

        assertEquals(h1, h2);
        assertNotEquals(h1, h3);
        assertEquals(h1.hashCode(), h2.hashCode());
    }
}
