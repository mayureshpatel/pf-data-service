package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
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

@Import(AccountRepository.class)
@DisplayName("AccountRepository Integration Tests (PostgreSQL)")
class AccountRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;
    private static final Long ACCOUNT_1 = 1L; // User 1 Main Checking

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
                    .currencyCode("USD")
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
        @DisplayName("should update an existing account with optimistic locking")
        void shouldUpdate() {
            // arrange
            Account account = accountRepository.findById(ACCOUNT_1).orElseThrow();
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .id(ACCOUNT_1)
                    .name("Updated Name")
                    .type("CHECKING")
                    .currencyCode("USD")
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
                    .currencyCode("USD")
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
                    .currencyCode("USD")
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
