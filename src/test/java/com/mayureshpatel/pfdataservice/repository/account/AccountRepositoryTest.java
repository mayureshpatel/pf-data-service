package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.BalanceMode;
import com.mayureshpatel.pfdataservice.dto.account.AccountCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code AccountRepository} against a real PostgreSQL instance (via {@link BaseRepositoryTest}'s Testcontainers setup and a shared baseline fixture), exercising the JDBC Client mapping, optimistic locking, and user-scoped ownership checks directly rather than mocking them. */
@Import(AccountRepository.class)
@DisplayName("AccountRepository Integration Tests (PostgreSQL)")
class AccountRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private org.springframework.jdbc.core.simple.JdbcClient jdbcClient;

    private static final Long USER_1 = 1L;
    private static final String CURRENCY_USD = "USD";
    private static final Long USER_2 = 2L;
    private static final Long ACCOUNT_1 = 1L; // User 1 Main Checking

    /** {@code findAll}/{@code findById}/{@code findAllByUserId}/{@code findByIdAndUserId} each read correctly against the baseline fixture, and {@code findByIdAndUserId} returns empty -- not the account -- when the id exists but belongs to a different user. */
    @Nested
    @DisplayName("Find Operations")
    class FindTests {
        @Test
        @DisplayName("should find all active accounts")
        void shouldFindAll() {
            // act
            List<Account> accounts = accountRepository.findAll();

            // assert & verify
            assertFalse(accounts.isEmpty());
            assertTrue(accounts.size() >= 4); // Based on baseline
        }

        @Test
        @DisplayName("should find account by ID")
        void shouldFindById() {
            // act
            Optional<Account> account = accountRepository.findById(ACCOUNT_1);

            // assert & verify
            assertTrue(account.isPresent());
            assertEquals("Main Checking", account.get().getName());
        }

        @Test
        @DisplayName("should find all accounts for a specific user")
        void shouldFindAllByUserId() {
            // act
            List<Account> accounts = accountRepository.findAllByUserId(USER_1);

            // assert & verify
            assertEquals(3, accounts.size());
            assertTrue(accounts.stream().allMatch(a -> a.getUserId().equals(USER_1)));
        }

        @Test
        @DisplayName("should find account by ID and User ID")
        void shouldFindByIdAndUserId() {
            // act
            Optional<Account> account = accountRepository.findByIdAndUserId(ACCOUNT_1, USER_1);

            // assert & verify
            assertTrue(account.isPresent());
            assertEquals(USER_1, account.get().getUserId());
        }

        @Test
        @DisplayName("should return empty if account ID exists but belongs to another user")
        void shouldNotFindByWrongUser() {
            // act
            Optional<Account> account = accountRepository.findByIdAndUserId(ACCOUNT_1, USER_2);

            // assert & verify
            assertTrue(account.isEmpty());
        }
    }

    /** {@code insert} returns the real database-generated id (checked against a fresh {@code count()}/{@code findById}, not just {@code update}'s rows-affected count, which is always 1 on a successful single-row insert and would otherwise coincidentally collide with baseline account 1); {@code update} enforces optimistic locking via {@code version} and user-scoped ownership, affecting zero rows on either mismatch rather than throwing; {@code deleteById} soft-deletes. */
    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should insert a new account")
        void shouldInsert() {
            // arrange
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .name("New Savings")
                    .type("SAVINGS")
                    .startingBalance(new BigDecimal("100.00"))
                    .currencyCode(CURRENCY_USD)
                    .bankName("Test Bank")
                    .build();

            // act
            int newId = accountRepository.insert(USER_1, request);

            // assert & verify -- must be the real generated id, not update()'s rows-affected count (which
            // is always 1 on a successful single-row insert and would coincidentally collide with
            // baseline account 1, "Main Checking", masking the bug this regresses against)
            long count = accountRepository.count();
            assertEquals(5, count);
            Account inserted = accountRepository.findById((long) newId).orElseThrow();
            assertEquals("New Savings", inserted.getName());
            assertEquals(USER_1, inserted.getUserId());
        }

        @Test
        @DisplayName("should round-trip balanceMode on insert and update, defaulting to DERIVED")
        void shouldRoundTripBalanceMode() {
            // arrange
            AccountCreateRequest manual = AccountCreateRequest.builder()
                    .name("Brokerage")
                    .type("INVESTMENT")
                    .startingBalance(new BigDecimal("500.00"))
                    .currencyCode(CURRENCY_USD)
                    .balanceMode("MANUAL")
                    .build();
            AccountCreateRequest derived = manual.toBuilder().name("Plain").balanceMode(null).build();

            // act
            int manualId = accountRepository.insert(USER_1, manual);
            int derivedId = accountRepository.insert(USER_1, derived);

            // assert & verify
            Account insertedManual = accountRepository.findById((long) manualId).orElseThrow();
            assertEquals(BalanceMode.MANUAL, insertedManual.getBalanceMode());
            assertTrue(insertedManual.isManualBalance());
            assertEquals(BalanceMode.DERIVED, accountRepository.findById((long) derivedId).orElseThrow().getBalanceMode());

            // act: flip the derived one to manual, then update again without a mode (keeps it)
            Account before = accountRepository.findById((long) derivedId).orElseThrow();
            AccountUpdateRequest flip = AccountUpdateRequest.builder()
                    .id((long) derivedId).name("Plain").type("INVESTMENT").currencyCode(CURRENCY_USD)
                    .balanceMode("MANUAL").version(before.getVersion()).build();
            assertEquals(1, accountRepository.update(USER_1, flip));
            Account flipped = accountRepository.findById((long) derivedId).orElseThrow();
            assertEquals(BalanceMode.MANUAL, flipped.getBalanceMode());
            AccountUpdateRequest keep = flip.toBuilder().balanceMode(null).version(flipped.getVersion()).build();
            assertEquals(1, accountRepository.update(USER_1, keep));
            assertEquals(BalanceMode.MANUAL, accountRepository.findById((long) derivedId).orElseThrow().getBalanceMode());
        }

        @Test
        @DisplayName("should compute moneyIn in the list query from contributions less transfers out and expenses")
        void shouldComputeMoneyIn() {
            // arrange
            int manualId = accountRepository.insert(USER_1, AccountCreateRequest.builder()
                    .name("Brokerage").type("INVESTMENT").startingBalance(new BigDecimal("1000.00"))
                    .startingBalance(BigDecimal.ZERO).currencyCode(CURRENCY_USD).balanceMode("MANUAL").build());
            insertTransaction(manualId, "CONTRIBUTION", "500.00", false);
            insertTransaction(manualId, "CONTRIBUTION", "500.00", false);
            insertTransaction(manualId, "EMPLOYER_CONTRIBUTION", "300.00", false);
            insertTransaction(manualId, "EARNINGS", "4.00", false);
            insertTransaction(manualId, "TRANSFER_OUT", "200.00", false);
            insertTransaction(manualId, "CONTRIBUTION", "9999.00", true);
            int emptyId = accountRepository.insert(USER_1, AccountCreateRequest.builder()
                    .name("Empty").type("INVESTMENT").startingBalance(BigDecimal.ZERO).currencyCode(CURRENCY_USD).balanceMode("MANUAL").build());

            // act
            List<Account> accounts = accountRepository.findAllByUserId(USER_1);

            // assert & verify
            Account manual = accounts.stream().filter(a -> a.getId() == manualId).findFirst().orElseThrow();
            assertEquals(0, new BigDecimal("800.00").compareTo(manual.getMoneyIn()));
            Account empty = accounts.stream().filter(a -> a.getId() == emptyId).findFirst().orElseThrow();
            assertEquals(0, BigDecimal.ZERO.compareTo(empty.getMoneyIn()));
        }

        @Test
        @DisplayName("should leave moneyIn null for a derived account")
        void shouldLeaveMoneyInNullForDerived() {
            // act
            List<Account> accounts = accountRepository.findAllByUserId(USER_1);

            // assert & verify
            assertTrue(accounts.stream().noneMatch(Account::isManualBalance));
            assertTrue(accounts.stream().allMatch(a -> a.getMoneyIn() == null));
        }

        @Test
        @DisplayName("should load the latest recorded balance in the list query, null when there is none")
        void shouldLoadLatestRecordedBalance() {
            // arrange
            int withHistory = accountRepository.insert(USER_1, AccountCreateRequest.builder()
                    .name("History").type("INVESTMENT").startingBalance(BigDecimal.ZERO).currencyCode(CURRENCY_USD).balanceMode("MANUAL").build());
            int withoutHistory = accountRepository.insert(USER_1, AccountCreateRequest.builder()
                    .name("None").type("INVESTMENT").startingBalance(BigDecimal.ZERO).currencyCode(CURRENCY_USD).balanceMode("MANUAL").build());
            jdbcClient.sql("insert into account_snapshots (account_id, snapshot_date, balance) values (:a, :d, :b)")
                    .param("a", withHistory).param("d", java.time.LocalDate.of(2026, 1, 1)).param("b", new BigDecimal("900.00")).update();
            jdbcClient.sql("insert into account_snapshots (account_id, snapshot_date, balance) values (:a, :d, :b)")
                    .param("a", withHistory).param("d", java.time.LocalDate.of(2026, 2, 1)).param("b", new BigDecimal("1000.00")).update();

            // act
            List<Account> accounts = accountRepository.findAllByUserId(USER_1);

            // assert & verify
            Account with = accounts.stream().filter(a -> a.getId() == withHistory).findFirst().orElseThrow();
            assertEquals(0, new BigDecimal("1000.00").compareTo(with.getLatestRecordedBalance()));
            Account without = accounts.stream().filter(a -> a.getId() == withoutHistory).findFirst().orElseThrow();
            assertNull(without.getLatestRecordedBalance());
        }

        private void insertTransaction(int accountId, String type, String amount, boolean deleted) {
            jdbcClient.sql("""
                            insert into transactions (account_id, amount, date, description, type, deleted_at)
                            values (:a, :amt, now(), 'seed', :t, case when :del then now() else null end)
                            """)
                    .param("a", accountId).param("amt", new BigDecimal(amount))
                    .param("t", type).param("del", deleted).update();
        }

        @Test
        @DisplayName("should update an existing account with optimistic locking")
        void shouldUpdate() {
            // arrange
            Account account = accountRepository.findById(ACCOUNT_1).orElseThrow();
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .id(ACCOUNT_1)
                    .name("Updated Name")
                    .type("CHECKING")
                    .currencyCode(CURRENCY_USD)
                    .bankName("New Bank")
                    .version(account.getVersion())
                    .build();

            // act
            int rows = accountRepository.update(USER_1, request);

            // assert & verify
            assertEquals(1, rows);
            Account updated = accountRepository.findById(ACCOUNT_1).orElseThrow();
            assertEquals("Updated Name", updated.getName());
            assertEquals(account.getVersion() + 1, updated.getVersion());
        }

        @Test
        @DisplayName("should fail update if version mismatch")
        void shouldFailUpdateOnVersionMismatch() {
            // arrange
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .id(ACCOUNT_1)
                    .name("Fail")
                    .type("CHECKING")
                    .currencyCode(CURRENCY_USD)
                    .version(999L) // Wrong version
                    .build();

            // act
            int rows = accountRepository.update(USER_1, request);

            // assert & verify
            assertEquals(0, rows);
        }

        @Test
        @DisplayName("should fail update if UserID mismatch")
        void shouldFailUpdateOnUserMismatch() {
            // arrange
            Account account = accountRepository.findById(ACCOUNT_1).orElseThrow();
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .id(ACCOUNT_1)
                    .name("Fail")
                    .type("CHECKING")
                    .currencyCode(CURRENCY_USD)
                    .version(account.getVersion())
                    .build();

            // act - User 2 trying to update User 1's account
            int rows = accountRepository.update(USER_2, request);

            // assert & verify
            assertEquals(0, rows);
        }

        @Test
        @DisplayName("should soft delete an account")
        void shouldDelete() {
            // act
            int rows = accountRepository.deleteById(ACCOUNT_1, USER_1);

            // assert & verify
            assertEquals(1, rows);
            Optional<Account> deleted = accountRepository.findById(ACCOUNT_1);
            assertTrue(deleted.isEmpty());
        }
    }

    /** {@code updateBalance} and {@code reconcile} both enforce optimistic locking, but differently: {@code updateBalance} affecting zero rows on a mismatch versus {@code reconcile} throwing {@link OptimisticLockingFailureException} outright, for either a wrong version or a wrong owning user. */
    @Nested
    @DisplayName("Balance Operations")
    class BalanceTests {
        @Test
        @DisplayName("should update balance explicitly")
        void shouldUpdateBalance() {
            // arrange
            Account account = accountRepository.findById(ACCOUNT_1).orElseThrow();

            // act
            int rows = accountRepository.updateBalance(USER_1, ACCOUNT_1, new BigDecimal("999.99"), account.getVersion());

            // assert & verify
            assertEquals(1, rows);
            Account updated = accountRepository.findById(ACCOUNT_1).orElseThrow();
            assertEquals(0, new BigDecimal("999.99").compareTo(updated.getCurrentBalance()));
            assertEquals(account.getVersion() + 1, updated.getVersion());
        }

        @Test
        @DisplayName("should reconcile balance with optimistic locking")
        void shouldReconcile() {
            // arrange
            Account account = accountRepository.findById(ACCOUNT_1).orElseThrow();

            // act
            int rows = accountRepository.reconcile(USER_1, ACCOUNT_1, new BigDecimal("1234.56"), account.getVersion());

            // assert & verify
            assertEquals(1, rows);
            Account updated = accountRepository.findById(ACCOUNT_1).orElseThrow();
            assertEquals(0, new BigDecimal("1234.56").compareTo(updated.getCurrentBalance()));
            assertEquals(account.getVersion() + 1, updated.getVersion());
        }

        @Test
        @DisplayName("should fail reconcile if version mismatch")
        void shouldFailReconcileOnVersionMismatch() {
            assertThrows(OptimisticLockingFailureException.class, () -> 
                accountRepository.reconcile(USER_1, ACCOUNT_1, BigDecimal.TEN, 999L)
            );
        }

        @Test
        @DisplayName("should fail reconcile if UserID mismatch")
        void shouldFailReconcileOnUserMismatch() {
            Account account = accountRepository.findById(ACCOUNT_1).orElseThrow();
            assertThrows(OptimisticLockingFailureException.class, () -> 
                accountRepository.reconcile(USER_2, ACCOUNT_1, BigDecimal.TEN, account.getVersion())
            );
        }
    }

    @Test
    @DisplayName("should count active accounts")
    void shouldCount() {
        // act
        long count = accountRepository.count();

        // assert & verify
        assertEquals(4, count);
    }
}
