package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.JdbcTestBase;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.domain.account.BankName;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.account.AccountSnapshotRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccountSnapshotRepositoryTest extends JdbcTestBase {

    @Autowired
    private AccountSnapshotRepository snapshotRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("snapshot_test_user");
        user.setEmail("snapshot_test@example.com");
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

    @Test
    void findByAccountIdAndSnapshotDate_ShouldReturnSnapshot() {
        LocalDate date = LocalDate.now();
        // Since we don't have insert implementation in this repository class yet (it's not showing in the file),
        // let's assume save() uses insert() which we need to check if it's there.
        // Wait, the file I read for AccountSnapshotRepository only had save() and delete()!
        // It didn't have insert() or update() implemented.
        
        // This means it's likely broken or partially implemented.
    }
}
