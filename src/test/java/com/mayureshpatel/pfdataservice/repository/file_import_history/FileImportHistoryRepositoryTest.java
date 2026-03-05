//package com.mayureshpatel.pfdataservice.repository.file_import_history;
//
//import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
//import com.mayureshpatel.pfdataservice.domain.account.Account;
//import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
//import com.mayureshpatel.pfdataservice.domain.user.User;
//import com.mayureshpatel.pfdataservice.util.TestDataFactory;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Transactional
//@DisplayName("FileImportHistoryRepository Integration Tests")
//class FileImportHistoryRepositoryTest extends BaseIntegrationTest {
//
//    @Autowired
//    private FileImportHistoryRepository fileImportHistoryRepository;
//
//    @Autowired
//    private TestDataFactory factory;
//
//    private User testUser;
//    private Account testAccount;
//
//    @BeforeEach
//    void setUp() {
//        testUser = factory.createUser("fih_" + System.currentTimeMillis());
//        testAccount = factory.createAccount(testUser, "Import Account");
//    }
//
//    @Test
//    @DisplayName("save() should persist new file import history")
//    void save_shouldPersistHistory() {
//        FileImportHistory history = factory.createFileImportHistory(testAccount, "test.csv", "abc123", 10);
//
//        assertThat(history).isNotNull();
//        assertThat(history.getFileName()).isEqualTo("test.csv");
//    }
//
//    @Test
//    @DisplayName("findByAccountIdAndFileHash() should detect duplicate import")
//    void findByAccountIdAndFileHash_shouldFindDuplicate() {
//        factory.createFileImportHistory(testAccount, "test.csv", "hash123", 5);
//
//        Optional<FileImportHistory> found = fileImportHistoryRepository.findByAccountIdAndFileHash(testAccount.getId(), "hash123");
//
//        assertThat(found).isPresent();
//        assertThat(found.get().getFileName()).isEqualTo("test.csv");
//        assertThat(found.get().getTransactionCount()).isEqualTo(5);
//    }
//
//    @Test
//    @DisplayName("findByAccountIdAndFileHash() should return empty when no match")
//    void findByAccountIdAndFileHash_shouldReturnEmpty() {
//        Optional<FileImportHistory> found = fileImportHistoryRepository.findByAccountIdAndFileHash(testAccount.getId(), "nonexistent");
//
//        assertThat(found).isEmpty();
//    }
//
//    @Test
//    @DisplayName("findByFileHash() should find by hash across all accounts")
//    void findByFileHash_shouldFindByHash() {
//        factory.createFileImportHistory(testAccount, "data.csv", "uniquehash", 20);
//
//        Optional<FileImportHistory> found = fileImportHistoryRepository.findByFileHash("uniquehash");
//
//        assertThat(found).isPresent();
//        assertThat(found.get().getTransactionCount()).isEqualTo(20);
//    }
//
//    @Test
//    @DisplayName("findByFileHash() should return empty when hash not found")
//    void findByFileHash_shouldReturnEmpty() {
//        assertThat(fileImportHistoryRepository.findByFileHash("nohash")).isEmpty();
//    }
//
//    @Test
//    @DisplayName("findAllByAccountId() should return all imports for account")
//    void findAllByAccountId_shouldReturnImports() {
//        factory.createFileImportHistory(testAccount, "file1.csv", "hash1", 5);
//        factory.createFileImportHistory(testAccount, "file2.csv", "hash2", 10);
//
//        List<FileImportHistory> imports = fileImportHistoryRepository.findAllByAccountId(testAccount.getId());
//
//        assertThat(imports).hasSize(2);
//    }
//
//    @Test
//    @DisplayName("findAllByAccountId() should return empty for account with no imports")
//    void findAllByAccountId_shouldReturnEmpty() {
//        Account otherAccount = factory.createAccount(testUser, "Empty Account");
//
//        List<FileImportHistory> imports = fileImportHistoryRepository.findAllByAccountId(otherAccount.getId());
//
//        assertThat(imports).isEmpty();
//    }
//}
