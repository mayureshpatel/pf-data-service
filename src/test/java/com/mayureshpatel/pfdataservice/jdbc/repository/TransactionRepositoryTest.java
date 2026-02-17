package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.JdbcTestBase;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.BankName;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionRepositoryTest extends JdbcTestBase {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("txn_test_user");
        user.setEmail("txn_test@example.com");
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

    private Transaction buildTransaction(String description) {
        Transaction transaction = new Transaction();
        transaction.setAccount(testAccount);
        transaction.setAmount(new BigDecimal("50.00"));
        transaction.setDate(LocalDate.now());
        transaction.setPostDate(LocalDate.now());
        transaction.setDescription(description);
        transaction.setOriginalVendorName(description);
        transaction.setVendorName(description);
        transaction.setType(TransactionType.EXPENSE);
        return transaction;
    }

    @Test
    void insert_ShouldCreateTransaction() {
        Transaction saved = transactionRepository.insert(buildTransaction("Starbucks"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDescription()).isEqualTo("Starbucks");
    }

    @Test
    void findById_ShouldReturnTransaction() {
        Transaction saved = transactionRepository.insert(buildTransaction("Starbucks"));

        Optional<Transaction> found = transactionRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Starbucks");
    }

    @Test
    void findByUserId_ShouldReturnUserTransactions() {
        transactionRepository.insert(buildTransaction("Txn 1"));
        transactionRepository.insert(buildTransaction("Txn 2"));

        List<Transaction> transactions = transactionRepository.findByUserId(testUser.getId());

        assertThat(transactions).hasSize(2);
    }

    @Test
    void update_ShouldUpdateTransaction() {
        Transaction saved = transactionRepository.insert(buildTransaction("Old"));
        saved.setDescription("New");

        Transaction updated = transactionRepository.update(saved);

        assertThat(updated.getDescription()).isEqualTo("New");
    }

    @Test
    void deleteById_ShouldSoftDelete() {
        Transaction saved = transactionRepository.insert(buildTransaction("Delete Me"));

        transactionRepository.deleteById(saved.getId());

        assertThat(transactionRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void count_ShouldReturnActiveCount() {
        long initial = transactionRepository.count();
        transactionRepository.insert(buildTransaction("Active"));
        transactionRepository.insert(buildTransaction("Active 2"));
        
        assertThat(transactionRepository.count()).isEqualTo(initial + 2);
    }
}
