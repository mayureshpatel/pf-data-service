package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.BankName;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class AccountRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        User newUser = new User();
        newUser.setUsername("acct_test_user");
        newUser.setEmail("acct_test@example.com");
        newUser.setPasswordHash("$2a$10$hashedpassword");
        testUser = userRepository.insert(newUser);
    }

    private Account buildAccount(String name, String type) {
        TableAudit audit = new TableAudit();
        audit.setCreatedBy(testUser);
        audit.setUpdatedBy(testUser);

        Account account = new Account();
        account.setName(name);
        account.setType(type);
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setCurrencyCode("USD");
        account.setBankName(BankName.CHASE);
        account.setUser(testUser);
        account.setAudit(audit);
        return account;
    }

    @Test
    void insert_ShouldCreateAccountAndReturnGeneratedId() {
        // When
        Account saved = accountRepository.insert(buildAccount("Checking", "CHECKING"));

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(1L);
        assertThat(saved.getName()).isEqualTo("Checking");
        assertThat(saved.getCurrentBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void save_NewAccount_ShouldInsert() {
        // When
        Account saved = accountRepository.save(buildAccount("Savings", "SAVINGS"));

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(1L);
    }

    @Test
    void save_ExistingAccount_ShouldUpdate() {
        // Given
        Account saved = accountRepository.insert(buildAccount("Old Name", "CHECKING"));
        saved.setName("New Name");
        saved.setCurrentBalance(new BigDecimal("2000.00"));

        // When
        Account updated = accountRepository.save(saved);

        // Then
        assertThat(updated.getVersion()).isEqualTo(2L);
        Optional<Account> found = accountRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("New Name");
        assertThat(found.get().getCurrentBalance()).isEqualByComparingTo("2000.00");
    }

    @Test
    void findById_ShouldReturnAccount_WhenExists() {
        // Given
        Account saved = accountRepository.insert(buildAccount("Checking", "CHECKING"));

        // When
        Optional<Account> found = accountRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Checking");
        assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<Account> found = accountRepository.findById(999999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findByUserId_ShouldReturnAllAccountsForUser() {
        // Given
        accountRepository.insert(buildAccount("Checking", "CHECKING"));
        accountRepository.insert(buildAccount("Savings", "SAVINGS"));

        // When
        List<Account> accounts = accountRepository.findByUserId(testUser.getId());

        // Then
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getName)
                .containsExactlyInAnyOrder("Checking", "Savings");
    }

    @Test
    void findAll_ShouldReturnAllNonDeletedAccounts() {
        // Given
        long initialCount = accountRepository.count();
        accountRepository.insert(buildAccount("Checking", "CHECKING"));

        // When
        List<Account> accounts = accountRepository.findAll();

        // Then
        assertThat(accounts).hasSizeGreaterThan((int) initialCount);
    }

    @Test
    void update_ShouldIncrementVersionAndPersistChanges() {
        // Given
        Account saved = accountRepository.insert(buildAccount("Original", "CHECKING"));
        saved.setName("Modified");

        // When
        Account updated = accountRepository.update(saved);

        // Then
        assertThat(updated.getVersion()).isEqualTo(2L);
        assertThat(accountRepository.findById(saved.getId()).get().getName()).isEqualTo("Modified");
    }

    @Test
    void update_WithStaleVersion_ShouldThrowException() {
        // Given
        Account saved = accountRepository.insert(buildAccount("Account", "CHECKING"));
        saved.setVersion(999L); // wrong version — simulates stale read

        // When / Then
        assertThatThrownBy(() -> accountRepository.update(saved))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Update failed");
    }

    @Test
    void deleteById_ShouldSoftDelete() {
        // Given
        Account saved = accountRepository.insert(buildAccount("To Delete", "CHECKING"));

        // When
        accountRepository.deleteById(saved.getId(), testUser.getId());

        // Then
        assertThat(accountRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void delete_ShouldSoftDelete() {
        // Given
        Account saved = accountRepository.insert(buildAccount("To Delete", "CHECKING"));

        // When
        accountRepository.delete(saved);

        // Then
        assertThat(accountRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void count_ShouldIncludeOnlyActiveAccounts() {
        // Given
        long before = accountRepository.count();
        Account a = accountRepository.insert(buildAccount("Active", "CHECKING"));
        Account b = accountRepository.insert(buildAccount("ToDelete", "CHECKING"));
        accountRepository.deleteById(b.getId(), testUser.getId());

        // When
        long after = accountRepository.count();

        // Then
        assertThat(after).isEqualTo(before + 1);
    }
}
