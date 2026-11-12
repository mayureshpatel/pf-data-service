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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code AccountSnapshotRepository} against a real PostgreSQL instance (via {@link BaseRepositoryTest}'s Testcontainers setup), exercising the JDBC Client mapping directly rather than mocking it. */
@Import(AccountSnapshotRepository.class)
@DisplayName("AccountSnapshotRepository Integration Tests (PostgreSQL)")
class AccountSnapshotRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AccountSnapshotRepository repository;

    private static final Long ACCOUNT_1 = 1L;

    /** {@code insert} returns the real database-generated id (checked against a fresh {@code findByAccountIdAndSnapshotDate} lookup, not just {@code update}'s rows-affected count), works whether the account is set via a flat {@code accountId} or an embedded {@link Account} object, and {@code delete}/{@code deleteById} both return 0 for a snapshot with no id rather than erroring. */
    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {
        @Test
        @DisplayName("should insert and find snapshot")
        void shouldInsertAndFind() {
            // arrange
            LocalDate date = LocalDate.of(2026, 3, 31);
            AccountSnapshot snapshot = AccountSnapshot.builder()
                    .accountId(ACCOUNT_1)
                    .snapshotDate(date)
                    .balance(new BigDecimal("1000.00"))
                    .build();

            // act
            int newId = repository.insert(snapshot);
            Optional<AccountSnapshot> result = repository.findByAccountIdAndSnapshotDate(ACCOUNT_1, date);

            // assert & verify -- must be the real generated id, not update()'s rows-affected count
            assertTrue(result.isPresent());
            assertEquals(0, new BigDecimal("1000.00").compareTo(result.get().getBalance()));
            assertEquals(result.get().getId(), (long) newId);
        }

        @Test
        @DisplayName("should insert using embedded account object")
        void shouldInsertWithEmbeddedAccount() {
            // arrange
            LocalDate date = LocalDate.of(2026, 4, 30);
            AccountSnapshot snapshot = AccountSnapshot.builder()
                    .account(Account.builder().id(ACCOUNT_1).build())
                    .snapshotDate(date)
                    .balance(new BigDecimal("2000.00"))
                    .build();

            // act
            int newId = repository.insert(snapshot);
            Optional<AccountSnapshot> result = repository.findByAccountIdAndSnapshotDate(ACCOUNT_1, date);

            // assert & verify -- must be the real generated id, not update()'s rows-affected count
            assertTrue(result.isPresent());
            assertEquals(result.get().getId(), (long) newId);
        }

        @Test
        @DisplayName("should update snapshot balance")
        void shouldUpdate() {
            // arrange
            LocalDate date = LocalDate.of(2026, 5, 31);
            AccountSnapshot snapshot = AccountSnapshot.builder()
                    .accountId(ACCOUNT_1)
                    .snapshotDate(date)
                    .balance(new BigDecimal("500.00"))
                    .build();
            repository.insert(snapshot);
            AccountSnapshot existing = repository.findByAccountIdAndSnapshotDate(ACCOUNT_1, date).orElseThrow();

            // act
            AccountSnapshot updated = existing.toBuilder().balance(new BigDecimal("750.00")).build();
            int rows = repository.update(updated);

            // assert & verify
            assertEquals(1, rows);
            AccountSnapshot result = repository.findById(existing.getId()).orElseThrow();
            assertEquals(0, new BigDecimal("750.00").compareTo(result.getBalance()));
        }

        @Test
        @DisplayName("should delete snapshot")
        void shouldDelete() {
            // arrange
            LocalDate date = LocalDate.of(2026, 6, 30);
            AccountSnapshot snapshot = AccountSnapshot.builder()
                    .accountId(ACCOUNT_1)
                    .snapshotDate(date)
                    .balance(new BigDecimal("100.00"))
                    .build();
            repository.insert(snapshot);
            AccountSnapshot existing = repository.findByAccountIdAndSnapshotDate(ACCOUNT_1, date).orElseThrow();

            // act
            int rows = repository.delete(existing);

            // assert & verify
            assertEquals(1, rows);
            assertTrue(repository.findById(existing.getId()).isEmpty());
        }

        @Test
        @DisplayName("should return 0 when deleting snapshot with no ID")
        void shouldHandleNoIdDelete() {
            assertEquals(0, repository.delete(AccountSnapshot.builder().build()));
        }

        @Test
        @DisplayName("should delete snapshot by ID directly")
        void shouldDeleteById() {
            // arrange
            LocalDate date = LocalDate.of(2026, 7, 31);
            AccountSnapshot snapshot = AccountSnapshot.builder()
                    .accountId(ACCOUNT_1)
                    .snapshotDate(date)
                    .balance(new BigDecimal("300.00"))
                    .build();
            repository.insert(snapshot);
            AccountSnapshot existing = repository.findByAccountIdAndSnapshotDate(ACCOUNT_1, date).orElseThrow();

            // act
            int rows = repository.deleteById(existing.getId());

            // assert & verify
            assertEquals(1, rows);
            assertTrue(repository.findById(existing.getId()).isEmpty());
        }
    }
    /** {@code upsert} keeps one snapshot per account and date (a second entry replaces the first), and the list/latest lookups order by date. */
    @Nested
    @DisplayName("Balance history")
    class BalanceHistoryTests {
        private static final Long EMPTY_ACCOUNT = 2L;

        @Test
        @DisplayName("upsert twice on the same date leaves one snapshot with the second balance")
        void shouldReplaceOnSameDate() {
            // arrange
            LocalDate date = LocalDate.of(2026, 11, 10);

            // act
            int first = repository.upsert(ACCOUNT_1, date, new BigDecimal("12000.00"));
            int second = repository.upsert(ACCOUNT_1, date, new BigDecimal("12500.00"));

            // assert & verify
            assertEquals(first, second);
            List<AccountSnapshot> all = repository.findAllByAccountIdNewestFirst(ACCOUNT_1);
            assertEquals(1, all.stream().filter(s -> date.equals(s.getSnapshotDate())).count());
            assertEquals(0, new BigDecimal("12500.00").compareTo(
                    repository.findByAccountIdAndSnapshotDate(ACCOUNT_1, date).orElseThrow().getBalance()));
        }

        @Test
        @DisplayName("findAllByAccountIdNewestFirst orders by date descending and findLatestSnapshotDate returns the newest")
        void shouldOrderNewestFirst() {
            // arrange
            repository.upsert(ACCOUNT_1, LocalDate.of(2026, 1, 5), new BigDecimal("1.00"));
            repository.upsert(ACCOUNT_1, LocalDate.of(2026, 3, 5), new BigDecimal("3.00"));
            repository.upsert(ACCOUNT_1, LocalDate.of(2026, 2, 5), new BigDecimal("2.00"));

            // act
            List<AccountSnapshot> all = repository.findAllByAccountIdNewestFirst(ACCOUNT_1);

            // assert & verify
            assertEquals(LocalDate.of(2026, 3, 5), all.get(0).getSnapshotDate());
            assertEquals(LocalDate.of(2026, 2, 5), all.get(1).getSnapshotDate());
            assertEquals(LocalDate.of(2026, 1, 5), all.get(2).getSnapshotDate());
            assertEquals(Optional.of(LocalDate.of(2026, 3, 5)), repository.findLatestSnapshotDate(ACCOUNT_1));
        }

        @Test
        @DisplayName("an account with no snapshots yields an empty list and no latest date")
        void shouldBeEmptyWithoutHistory() {
            // act & assert & verify
            assertTrue(repository.findAllByAccountIdNewestFirst(999_999L).isEmpty());
            assertTrue(repository.findLatestSnapshotDate(999_999L).isEmpty());
        }
    }
}
