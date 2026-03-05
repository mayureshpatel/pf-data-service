package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountSnapshot Domain Object Tests")
class AccountSnapshotTest {

    @Test
    @DisplayName("Builder should correctly populate all fields")
    void builder_shouldPopulateFields() {
        TableAudit audit = TableAudit.insertAudit(null);
        Account account = Account.builder().id(10L).build();
        LocalDate now = LocalDate.now();
        
        AccountSnapshot snapshot = AccountSnapshot.builder()
                .id(1L)
                .accountId(10L)
                .account(account)
                .snapshotDate(now)
                .balance(new BigDecimal("1234.56"))
                .audit(audit)
                .build();

        assertEquals(1L, snapshot.getId());
        assertEquals(10L, snapshot.getAccountId());
        assertEquals(account, snapshot.getAccount());
        assertEquals(now, snapshot.getSnapshotDate());
        assertEquals(new BigDecimal("1234.56"), snapshot.getBalance());
        assertEquals(audit, snapshot.getAudit());
    }

    @Test
    @DisplayName("toBuilder should create a mutable copy")
    void toBuilder_shouldCreateMutableCopy() {
        AccountSnapshot original = AccountSnapshot.builder()
                .id(1L)
                .balance(BigDecimal.ZERO)
                .build();

        AccountSnapshot modified = original.toBuilder()
                .balance(BigDecimal.TEN)
                .build();

        assertNotSame(original, modified);
        assertEquals(BigDecimal.TEN, modified.getBalance());
        assertEquals(BigDecimal.ZERO, original.getBalance());
    }

    @Test
    @DisplayName("Equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        AccountSnapshot s1 = AccountSnapshot.builder().id(1L).balance(BigDecimal.ONE).build();
        AccountSnapshot s2 = AccountSnapshot.builder().id(1L).balance(BigDecimal.TEN).build();
        AccountSnapshot s3 = AccountSnapshot.builder().id(2L).build();

        assertEquals(s1, s2);
        assertNotEquals(s1, s3);
        assertEquals(s1.hashCode(), s2.hashCode());
    }
}
