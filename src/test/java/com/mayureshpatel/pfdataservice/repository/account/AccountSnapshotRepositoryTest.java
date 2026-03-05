package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(AccountSnapshotRepository.class)
@DisplayName("AccountSnapshotRepository Integration Tests (PostgreSQL)")
class AccountSnapshotRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AccountSnapshotRepository repository;

    private static final Long ACCOUNT_1 = 1L;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {
        @Test
        @DisplayName("should insert and find snapshot")
        void shouldInsertAndFind() {
            // Arrange
            LocalDate date = LocalDate.of(2026, 3, 31);
            AccountSnapshot snapshot = AccountSnapshot.builder()
                    .accountId(ACCOUNT_1)
                    .snapshotDate(date)
                    .balance(new BigDecimal("1000.00"))
                    .build();

            // Act
            int rows = repository.insert(snapshot);
            Optional<AccountSnapshot> result = repository.findByAccountIdAndSnapshotDate(ACCOUNT_1, date);

            // Assert
            assertEquals(1, rows);
            assertTrue(result.isPresent());
            assertEquals(0, new BigDecimal("1000.00").compareTo(result.get().getBalance()));
        }

        @Test
        @DisplayName("should insert using embedded account object")
        void shouldInsertWithEmbeddedAccount() {
            // Arrange
            LocalDate date = LocalDate.of(2026, 4, 30);
            AccountSnapshot snapshot = AccountSnapshot.builder()
                    .account(Account.builder().id(ACCOUNT_1).build())
                    .snapshotDate(date)
                    .balance(new BigDecimal("2000.00"))
                    .build();

            // Act
            int rows = repository.insert(snapshot);
            Optional<AccountSnapshot> result = repository.findByAccountIdAndSnapshotDate(ACCOUNT_1, date);

            // Assert
            assertEquals(1, rows);
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("should update snapshot balance")
        void shouldUpdate() {
            // Arrange
            LocalDate date = LocalDate.of(2026, 5, 31);
            AccountSnapshot snapshot = AccountSnapshot.builder()
                    .accountId(ACCOUNT_1)
                    .snapshotDate(date)
                    .balance(new BigDecimal("500.00"))
                    .build();
            repository.insert(snapshot);
            AccountSnapshot existing = repository.findByAccountIdAndSnapshotDate(ACCOUNT_1, date).orElseThrow();

            // Act
            AccountSnapshot updated = existing.toBuilder().balance(new BigDecimal("750.00")).build();
            int rows = repository.update(updated);

            // Assert
            assertEquals(1, rows);
            AccountSnapshot result = repository.findById(existing.getId()).orElseThrow();
            assertEquals(0, new BigDecimal("750.00").compareTo(result.getBalance()));
        }

        @Test
        @DisplayName("should delete snapshot")
        void shouldDelete() {
            // Arrange
            LocalDate date = LocalDate.of(2026, 6, 30);
            AccountSnapshot snapshot = AccountSnapshot.builder()
                    .accountId(ACCOUNT_1)
                    .snapshotDate(date)
                    .balance(new BigDecimal("100.00"))
                    .build();
            repository.insert(snapshot);
            AccountSnapshot existing = repository.findByAccountIdAndSnapshotDate(ACCOUNT_1, date).orElseThrow();

            // Act
            int rows = repository.delete(existing);

            // Assert
            assertEquals(1, rows);
            assertTrue(repository.findById(existing.getId()).isEmpty());
        }

        @Test
        @DisplayName("should return 0 when deleting snapshot with no ID")
        void shouldHandleNoIdDelete() {
            assertEquals(0, repository.delete(AccountSnapshot.builder().build()));
        }
    }
}
