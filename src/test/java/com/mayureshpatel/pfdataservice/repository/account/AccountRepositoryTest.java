//package com.mayureshpatel.pfdataservice.repository.account;
//
//import com.mayureshpatel.pfdataservice.config.TestContainersConfig;
//import com.mayureshpatel.pfdataservice.domain.TableAudit;
//import com.mayureshpatel.pfdataservice.domain.account.Account;
//import com.mayureshpatel.pfdataservice.domain.account.AccountType;
//import com.mayureshpatel.pfdataservice.domain.bank.BankName;
//import com.mayureshpatel.pfdataservice.domain.currency.Currency;
//import com.mayureshpatel.pfdataservice.domain.user.User;
//import com.mayureshpatel.pfdataservice.repository.account.mapper.AccountRowMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.jdbc.core.simple.JdbcClient;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@JdbcTest
//@Import({AccountRepository.class, AccountRowMapper.class, TestContainersConfig.class})
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@ActiveProfiles("test")
//@Transactional
//@DisplayName("AccountRepository Slice Tests")
//class AccountRepositoryTest {
//
//    @Autowired
//    private AccountRepository accountRepository;
//
//    @Autowired
//    private JdbcClient jdbcClient;
//
//    private User testUser;
//    private Currency testCurrency;
//    private AccountType testAccountType;
//
//    @BeforeEach
//    void setUp() {
//        // Setup User via raw SQL to keep the slice thin
//        long userId = 999L;
//        jdbcClient.sql("INSERT INTO users (id, username, email, password_hash, last_updated_by) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING")
//                .params(userId, "testuser", "test@example.com", "hash", "system")
//                .update();
//
//        testUser = new User();
//        testUser.setId(userId);
//        testUser.setUsername("testuser");
//
//        // Setup Currency
//        jdbcClient.sql("INSERT INTO currencies (code, name, symbol, is_active) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING")
//                .params("USD", "US Dollar", "$", true)
//                .update();
//
//        testCurrency = new Currency();
//        testCurrency.setCode("USD");
//
//        // Setup AccountType
//        jdbcClient.sql("INSERT INTO account_types (code, label, icon, color, is_asset, sort_order, is_active) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING")
//                .params("CHECKING", "Checking", "pi-wallet", "text-blue-600", true, 1, true)
//                .update();
//
//        testAccountType = new AccountType();
//        testAccountType.setCode("CHECKING");
//    }
//
//    @Test
//    @DisplayName("should insert and find account by id")
//    void insertAndFindById() {
//        Account account = new Account();
//        account.setName("Test Account");
//        account.setType(testAccountType);
//        account.setCurrentBalance(new BigDecimal("1000.00"));
//        account.setCurrency(testCurrency);
//        account.setBankName(BankName.CAPITAL_ONE);
//        account.setUser(testUser);
//        account.setAudit(new TableAudit());
//        account.getAudit().setCreatedBy(testUser);
//
//        Account saved = accountRepository.save(account);
//        Optional<Account> found = accountRepository.findById(saved.getId());
//
//        assertThat(found).isPresent();
//        assertThat(found.get().getName()).isEqualTo("Test Account");
//        assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
//        assertThat(found.get().getBankName()).isEqualTo(BankName.CAPITAL_ONE);
//    }
//
//    @Test
//    @DisplayName("should find all accounts by user id")
//    void findAllByUserId() {
//        Account acc1 = createAccount("Acc 1");
//        Account acc2 = createAccount("Acc 2");
//        accountRepository.save(acc1);
//        accountRepository.save(acc2);
//
//        List<Account> accounts = accountRepository.findAllByUserId(testUser.getId());
//
//        assertThat(accounts).hasSize(2);
//        assertThat(accounts).extracting(Account::getName).containsExactlyInAnyOrder("Acc 1", "Acc 2");
//    }
//
//    @Test
//    @DisplayName("should update account name and balance")
//    void updateAccount() {
//        Account account = createAccount("Old Name");
//        Account saved = accountRepository.save(account);
//
//        saved.setName("New Name");
//        saved.setCurrentBalance(new BigDecimal("2000.00"));
//        accountRepository.save(saved);
//
//        Account updated = accountRepository.findById(saved.getId()).get();
//        assertThat(updated.getName()).isEqualTo("New Name");
//        assertThat(updated.getCurrentBalance()).isEqualByComparingTo("2000.00");
//        assertThat(updated.getVersion()).isEqualTo(2L);
//    }
//
//    @Test
//    @DisplayName("should soft delete account")
//    void deleteAccount() {
//        Account account = createAccount("To Delete");
//        Account saved = accountRepository.save(account);
//        long initialCount = accountRepository.count();
//
//        accountRepository.delete(saved);
//
//        assertThat(accountRepository.findById(saved.getId())).isEmpty();
//        assertThat(accountRepository.count()).isEqualTo(initialCount - 1);
//    }
//
//    @Test
//    @DisplayName("should find account by accountId and userId")
//    void findByIdAndUserId() {
//        Account account = createAccount("Owned Account");
//        Account saved = accountRepository.save(account);
//
//        Optional<Account> found = accountRepository.findByIdAndUserId(saved.getId(), testUser.getId());
//
//        assertThat(found).isPresent();
//        assertThat(found.get().getName()).isEqualTo("Owned Account");
//    }
//
//    private Account createAccount(String name) {
//        Account account = new Account();
//        account.setName(name);
//        account.setType(testAccountType);
//        account.setCurrentBalance(new BigDecimal("1000.00"));
//        account.setCurrency(testCurrency);
//        account.setUser(testUser);
//        account.setAudit(new TableAudit());
//        account.getAudit().setCreatedBy(testUser);
//        account.getAudit().setUpdatedBy(testUser);
//        return account;
//    }
//}
