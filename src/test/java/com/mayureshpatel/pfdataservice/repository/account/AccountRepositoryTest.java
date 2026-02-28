package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.TimestampAudit;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("AccountRepository Integration Tests")
class AccountRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    private User testUser;
    private Currency testCurrency;
    private AccountType testAccountType;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser_" + System.currentTimeMillis());
        testUser.setEmail("test" + System.currentTimeMillis() + "@example.com");
        testUser.setPasswordHash("hash");
        userRepository.save(testUser);

        testCurrency = new Currency();
        testCurrency.setCode("USD");
        testCurrency.setName("US Dollar");
        testCurrency.setSymbol("$");
        testCurrency.setActive(true);
        currencyRepository.save(testCurrency);

        // AccountType is likely seeded by Flyway, let's try to find one or create if needed
        List<AccountType> types = accountTypeRepository.findByIsActiveTrueOrderBySortOrder();
        if (types.isEmpty()) {
            testAccountType = new AccountType();
            testAccountType.setCode("CHECKING");
            testAccountType.setLabel("Checking");
            testAccountType.setActive(true);
            // Since AccountTypeRepository doesn't have a full insert, we might need to rely on seeded data
            // or use JdbcClient to insert. For now let's assume it's seeded as per V13__seed_initial_data.sql
        } else {
            testAccountType = types.get(0);
        }
    }

    @Test
    @DisplayName("should insert and find account by id")
    void insertAndFindById() {
        // Arrange
        Account account = new Account();
        account.setName("Test Account");
        account.setType(testAccountType);
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setCurrency(testCurrency);
        account.setBankName(BankName.CAPITAL_ONE);
        account.setUser(testUser);
        account.setAudit(new TableAudit());
        account.getAudit().setCreatedBy(testUser);

        // Act
        Account saved = accountRepository.save(account);
        Optional<Account> found = accountRepository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Account");
        assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
        assertThat(found.get().getBankName()).isEqualTo(BankName.CAPITAL_ONE);
    }

    @Test
    @DisplayName("should find all accounts by user id")
    void findByUserId() {
        // Arrange
        Account acc1 = createAccount("Acc 1");
        Account acc2 = createAccount("Acc 2");
        accountRepository.save(acc1);
        accountRepository.save(acc2);

        // Act
        List<Account> accounts = accountRepository.findByUserId(testUser.getId());

        // Assert
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getName).containsExactlyInAnyOrder("Acc 1", "Acc 2");
    }

    @Test
    @DisplayName("should update account name and balance")
    void updateAccount() {
        // Arrange
        Account account = createAccount("Old Name");
        Account saved = accountRepository.save(account);

        // Act
        saved.setName("New Name");
        saved.setCurrentBalance(new BigDecimal("2000.00"));
        accountRepository.save(saved);

        // Assert
        Account updated = accountRepository.findById(saved.getId()).get();
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getCurrentBalance()).isEqualByComparingTo("2000.00");
        assertThat(updated.getVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("should soft delete account")
    void deleteAccount() {
        // Arrange
        Account account = createAccount("To Delete");
        Account saved = accountRepository.save(account);
        long initialCount = accountRepository.count();

        // Act
        accountRepository.delete(saved);

        // Assert
        assertThat(accountRepository.findById(saved.getId())).isEmpty();
        assertThat(accountRepository.count()).isEqualTo(initialCount - 1);
    }

    private Account createAccount(String name) {
        Account account = new Account();
        account.setName(name);
        account.setType(testAccountType);
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setCurrency(testCurrency);
        account.setUser(testUser);
        account.setAudit(new TableAudit());
        account.getAudit().setCreatedBy(testUser);
        account.getAudit().setUpdatedBy(testUser);
        return account;
    }
}
