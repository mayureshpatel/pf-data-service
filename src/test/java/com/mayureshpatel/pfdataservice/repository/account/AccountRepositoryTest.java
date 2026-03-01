package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
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

        List<AccountType> types = accountTypeRepository.findByIsActiveTrueOrderBySortOrder();
        if (types.isEmpty()) {
            testAccountType = new AccountType();
            testAccountType.setCode("CHECKING");
            testAccountType.setLabel("Checking");
            testAccountType.setActive(true);
        } else {
            testAccountType = types.get(0);
        }
    }

    @Test
    @DisplayName("should insert and find account by id")
    void insertAndFindById() {
        Account account = new Account();
        account.setName("Test Account");
        account.setType(testAccountType);
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setCurrency(testCurrency);
        account.setBankName(BankName.CAPITAL_ONE);
        account.setUser(testUser);
        account.setAudit(new TableAudit());
        account.getAudit().setCreatedBy(testUser);

        Account saved = accountRepository.save(account);
        Optional<Account> found = accountRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Account");
        assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
        assertThat(found.get().getBankName()).isEqualTo(BankName.CAPITAL_ONE);
    }

    @Test
    @DisplayName("should find all accounts by user id")
    void findByUserId() {
        Account acc1 = createAccount("Acc 1");
        Account acc2 = createAccount("Acc 2");
        accountRepository.save(acc1);
        accountRepository.save(acc2);

        List<Account> accounts = accountRepository.findByUserId(testUser.getId());

        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getName).containsExactlyInAnyOrder("Acc 1", "Acc 2");
    }

    @Test
    @DisplayName("should update account name and balance")
    void updateAccount() {
        Account account = createAccount("Old Name");
        Account saved = accountRepository.save(account);

        saved.setName("New Name");
        saved.setCurrentBalance(new BigDecimal("2000.00"));
        accountRepository.save(saved);

        Account updated = accountRepository.findById(saved.getId()).get();
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getCurrentBalance()).isEqualByComparingTo("2000.00");
        assertThat(updated.getVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("should soft delete account")
    void deleteAccount() {
        Account account = createAccount("To Delete");
        Account saved = accountRepository.save(account);
        long initialCount = accountRepository.count();

        accountRepository.delete(saved);

        assertThat(accountRepository.findById(saved.getId())).isEmpty();
        assertThat(accountRepository.count()).isEqualTo(initialCount - 1);
    }

    @Test
    @DisplayName("should find account by accountId and userId")
    void findByAccountIdAndUserId() {
        Account account = createAccount("Owned Account");
        Account saved = accountRepository.save(account);

        Optional<Account> found = accountRepository.findByAccountIdAndUserId(saved.getId(), testUser.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Owned Account");
    }

    @Test
    @DisplayName("should return empty when accountId exists but userId does not match")
    void findByAccountIdAndUserId_wrongUser() {
        Account account = createAccount("Other Account");
        Account saved = accountRepository.save(account);

        User otherUser = new User();
        otherUser.setUsername("otheruser_" + System.currentTimeMillis());
        otherUser.setEmail("other" + System.currentTimeMillis() + "@example.com");
        otherUser.setPasswordHash("hash");
        userRepository.save(otherUser);

        Optional<Account> found = accountRepository.findByAccountIdAndUserId(saved.getId(), otherUser.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should isolate accounts between users")
    void dataIsolation_userCannotSeeOtherUsersAccounts() {
        Account acc1 = createAccount("User1 Account");
        accountRepository.save(acc1);

        User otherUser = new User();
        otherUser.setUsername("isolated_" + System.currentTimeMillis());
        otherUser.setEmail("isolated" + System.currentTimeMillis() + "@example.com");
        otherUser.setPasswordHash("hash");
        userRepository.save(otherUser);

        Account acc2 = new Account();
        acc2.setName("User2 Account");
        acc2.setType(testAccountType);
        acc2.setCurrentBalance(new BigDecimal("500.00"));
        acc2.setCurrency(testCurrency);
        acc2.setUser(otherUser);
        acc2.setAudit(new TableAudit());
        acc2.getAudit().setCreatedBy(otherUser);
        accountRepository.save(acc2);

        List<Account> user1Accounts = accountRepository.findByUserId(testUser.getId());
        List<Account> user2Accounts = accountRepository.findByUserId(otherUser.getId());

        assertThat(user1Accounts).extracting(Account::getName).containsExactly("User1 Account");
        assertThat(user2Accounts).extracting(Account::getName).containsExactly("User2 Account");
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
