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
            // Act
            List<Account> accounts = accountRepository.findAll();

            // Assert
            assertFalse(accounts.isEmpty());
            assertTrue(accounts.size() >= 4); // Based on baseline
        }

        @Test
        @DisplayName("should find account by ID")
        void shouldFindById() {
            // Act
            Optional<Account> account = accountRepository.findById(ACCOUNT_1);

            // Assert
            assertTrue(account.isPresent());
            assertEquals("Main Checking", account.get().getName());
        }

        @Test
        @DisplayName("should find all accounts for a specific user")
        void shouldFindAllByUserId() {
            // Act
            List<Account> accounts = accountRepository.findAllByUserId(USER_1);

            // Assert
            assertEquals(3, accounts.size());
            assertTrue(accounts.stream().allMatch(a -> a.getUserId().equals(USER_1)));
        }

        @Test
        @DisplayName("should find account by ID and User ID")
        void shouldFindByIdAndUserId() {
            // Act
            Optional<Account> account = accountRepository.findByIdAndUserId(ACCOUNT_1, USER_1);

            // Assert
            assertTrue(account.isPresent());
            assertEquals(USER_1, account.get().getUserId());
        }

        @Test
        @DisplayName("should return empty if account ID exists but belongs to another user")
        void shouldNotFindByWrongUser() {
            // Act
            Optional<Account> account = accountRepository.findByIdAndUserId(ACCOUNT_1, USER_2);

            // Assert
            assertTrue(account.isEmpty());
        }
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should insert a new account")
        void shouldInsert() {
            // Arrange
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .name("New Savings")
                    .type("SAVINGS")
                    .startingBalance(new BigDecimal("100.00"))
                    .currencyCode("USD")
                    .bankName("Test Bank")
                    .build();

            // Act
            int rows = accountRepository.insert(USER_1, request);

            // Assert
            assertEquals(1, rows);
            long count = accountRepository.count();
            assertEquals(5, count);
        }

        @Test
        @DisplayName("should update an existing account with optimistic locking")
        void shouldUpdate() {
            // Arrange
            Account account = accountRepository.findById(ACCOUNT_1).orElseThrow();
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .id(ACCOUNT_1)
                    .name("Updated Name")
                    .type("CHECKING")
                    .currencyCode("USD")
                    .bankName("New Bank")
                    .version(account.getVersion())
                    .build();

            // Act
            int rows = accountRepository.update(USER_1, request);

            // Assert
            assertEquals(1, rows);
            Account updated = accountRepository.findById(ACCOUNT_1).orElseThrow();
            assertEquals("Updated Name", updated.getName());
            assertEquals(account.getVersion() + 1, updated.getVersion());
        }

        @Test
        @DisplayName("should fail update if version mismatch")
        void shouldFailUpdateOnVersionMismatch() {
            // Arrange
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .id(ACCOUNT_1)
                    .name("Fail")
                    .type("CHECKING")
                    .currencyCode("USD")
                    .version(999L) // Wrong version
                    .build();

            // Act
            int rows = accountRepository.update(USER_1, request);

            // Assert
            assertEquals(0, rows);
        }

        @Test
        @DisplayName("should soft delete an account")
        void shouldDelete() {
            // Act
            int rows = accountRepository.deleteById(ACCOUNT_1, USER_1);

            // Assert
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
            // Arrange
            Account account = accountRepository.findById(ACCOUNT_1).orElseThrow();

            // Act
            int rows = accountRepository.updateBalance(USER_1, ACCOUNT_1, new BigDecimal("999.99"), account.getVersion());

            // Assert
            assertEquals(1, rows);
            Account updated = accountRepository.findById(ACCOUNT_1).orElseThrow();
            assertEquals(0, new BigDecimal("999.99").compareTo(updated.getCurrentBalance()));
            assertEquals(account.getVersion() + 1, updated.getVersion());
        }

        @Test
        @DisplayName("should reconcile balance")
        void shouldReconcile() {
            // Act
            int rows = accountRepository.reconcile(USER_1, ACCOUNT_1, new BigDecimal("1234.56"));

            // Assert
            assertEquals(1, rows);
            Account updated = accountRepository.findById(ACCOUNT_1).orElseThrow();
            assertEquals(0, new BigDecimal("1234.56").compareTo(updated.getCurrentBalance()));
        }
    }

    @Test
    @DisplayName("should count active accounts")
    void shouldCount() {
        // Act
        long count = accountRepository.count();

        // Assert
        assertEquals(4, count);
    }
}
