package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.JdbcTestBase;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.BankName;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.vendor.Vendor;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringTransactionRepositoryTest extends JdbcTestBase {

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("recurring_test_user");
        user.setEmail("recurring_test@example.com");
        user.setPasswordHash("hash");
        testUser = userRepository.insert(user);

        Account account = new Account();
        account.setName("Test Account");
        account.setType("CHECKING");
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setCurrencyCode("USD");
        account.setBankName(BankName.CHASE);
        account.setUser(testUser);
        
        TableAudit audit = new TableAudit();
        audit.setCreatedBy(testUser);
        audit.setUpdatedBy(testUser);
        account.setAudit(audit);
        
        testAccount = accountRepository.insert(account);
    }

    private RecurringTransaction buildRecurring(String merchant) {
        Vendor vendor = new Vendor();
        vendor.setName(merchant);

        return RecurringTransaction.builder()
                .user(testUser)
                .account(testAccount)
                .vendor(vendor)
                .amount(new BigDecimal("15.99"))
                .frequency(Frequency.MONTHLY)
                .lastDate(LocalDate.now().minusMonths(1))
                .nextDate(LocalDate.now())
                .active(true)
                .build();
    }

    @Test
    void insert_ShouldCreateRecurringTransaction() {
        RecurringTransaction recurring = buildRecurring("Netflix");
        RecurringTransaction saved = recurringRepository.insert(recurring);

        assertThat(saved).isNotNull();
        // Since insert doesn't return generated ID in this specific repository implementation (it doesn't use KeyHolder)
        // We might need to check if it was actually inserted via findAllByUserId
        List<RecurringTransaction> all = recurringRepository.findAllByUserId(testUser.getId());
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getVendor().getName()).isEqualTo("Netflix");
    }

    @Test
    void findAllByUserId_ShouldReturnUserTransactions() {
        recurringRepository.insert(buildRecurring("Netflix"));
        recurringRepository.insert(buildRecurring("Spotify"));

        List<RecurringTransaction> all = recurringRepository.findAllByUserId(testUser.getId());

        assertThat(all).hasSize(2);
    }

    @Test
    void update_ShouldUpdateTransaction() {
        RecurringTransaction recurring = buildRecurring("Old");
        recurringRepository.insert(recurring);
        
        List<RecurringTransaction> all = recurringRepository.findAllByUserId(testUser.getId());
        RecurringTransaction saved = all.get(0);
        
        Vendor newVendor = new Vendor();
        newVendor.setName("New");
        saved.setVendor(newVendor);
        saved.setAmount(new BigDecimal("20.00"));

        recurringRepository.update(saved);

        List<RecurringTransaction> updatedAll = recurringRepository.findAllByUserId(testUser.getId());
        assertThat(updatedAll.get(0).getVendor().getName()).isEqualTo("New");
        assertThat(updatedAll.get(0).getAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void delete_ShouldRemoveTransaction() {
        RecurringTransaction recurring = buildRecurring("ToDelete");
        recurringRepository.insert(recurring);
        
        List<RecurringTransaction> all = recurringRepository.findAllByUserId(testUser.getId());
        Long id = all.get(0).getId();

        recurringRepository.delete(id, testUser.getId());

        assertThat(recurringRepository.findAllByUserId(testUser.getId())).isEmpty();
    }
}
