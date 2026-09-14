package com.mayureshpatel.pfdataservice.repository.file_import_history;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.dto.transaction.fileimport.FileImportCreateRequest;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(FileImportHistoryRepository.class)
@DisplayName("FileImportHistoryRepository Integration Tests (PostgreSQL)")
class FileImportHistoryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private FileImportHistoryRepository repository;

    private static final Long ACCOUNT_1 = 1L;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {
        @Test
        @DisplayName("should insert and find file import history")
        void shouldInsertAndFind() {
            // arrange
            FileImportCreateRequest request = FileImportCreateRequest.builder()
                    .accountId(String.valueOf(ACCOUNT_1))
                    .fileHash("hash123")
                    .fileName("test.csv")
                    .build();

            // act
            int newId = repository.insert(request);
            Optional<FileImportHistory> result = repository.findByAccountIdAndFileHash(ACCOUNT_1, "hash123");

            // assert & verify -- must be the real generated id, not update()'s rows-affected count
            assertTrue(result.isPresent());
            assertEquals("test.csv", result.get().getFileName());
            assertEquals(result.get().getId(), (long) newId);
        }

        @Test
        @DisplayName("should find by ID")
        void shouldFindById() {
            // arrange
            repository.insert(FileImportCreateRequest.builder().accountId(String.valueOf(ACCOUNT_1)).fileHash("h3").fileName("3.csv").build());
            Long id = repository.findByAccountIdAndFileHash(ACCOUNT_1, "h3").get().getId();

            // act
            Optional<FileImportHistory> result = repository.findById(id);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals(id, result.get().getId());
        }

        @Test
        @DisplayName("should save a file import history built from a domain object")
        void shouldSave() {
            // arrange
            FileImportHistory history = FileImportHistory.builder()
                    .account(Account.builder().id(ACCOUNT_1).build())
                    .fileHash("h5")
                    .fileName("5.csv")
                    .transactionCount(3)
                    .build();

            // act
            int newId = repository.save(history);

            // assert & verify -- must be the real generated id, not update()'s rows-affected count
            FileImportHistory saved = repository.findByAccountIdAndFileHash(ACCOUNT_1, "h5").orElseThrow();
            assertEquals("5.csv", saved.getFileName());
            assertEquals(3, saved.getTransactionCount());
            assertEquals(saved.getId(), (long) newId);
        }
    }
}
