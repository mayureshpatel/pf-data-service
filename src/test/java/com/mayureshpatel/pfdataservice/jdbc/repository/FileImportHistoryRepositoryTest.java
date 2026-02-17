package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.JdbcTestBase;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.BankName;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.file_import_history.FileImportHistoryRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FileImportHistoryRepositoryTest extends JdbcTestBase {

    @Autowired
    private FileImportHistoryRepository historyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("history_test_user");
        user.setEmail("history_test@example.com");
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

    private FileImportHistory buildHistory(String fileName, String hash) {
        FileImportHistory history = new FileImportHistory();
        history.setAccount(testAccount);
        history.setFileName(fileName);
        history.setFileHash(hash);
        history.setTransactionCount(10);
        return history;
    }

    @Test
    void insert_ShouldCreateHistory() {
        FileImportHistory saved = historyRepository.insert(buildHistory("test.csv", "hash123"));

        assertThat(saved).isNotNull();
        // Since insert doesn't return ID in this implementation, we check by fileHash
        Optional<FileImportHistory> found = historyRepository.findByFileHash("hash123");
        assertThat(found).isPresent();
        assertThat(found.get().getFileName()).isEqualTo("test.csv");
    }

    @Test
    void findAllByAccountId_ShouldReturnHistories() {
        historyRepository.insert(buildHistory("file1.csv", "hash1"));
        historyRepository.insert(buildHistory("file2.csv", "hash2"));

        List<FileImportHistory> all = historyRepository.findAllByAccountId(testAccount.getId());

        assertThat(all).hasSize(2);
    }

    @Test
    void findByFileHash_ShouldReturnHistory() {
        historyRepository.insert(buildHistory("test.csv", "unique_hash"));

        Optional<FileImportHistory> found = historyRepository.findByFileHash("unique_hash");

        assertThat(found).isPresent();
    }

    @Test
    void deleteById_ShouldRemoveHistory() {
        historyRepository.insert(buildHistory("delete.csv", "hash_del"));
        FileImportHistory saved = historyRepository.findByFileHash("hash_del").get();

        historyRepository.deleteById(saved.getId());

        assertThat(historyRepository.findByFileHash("hash_del")).isEmpty();
    }
}
