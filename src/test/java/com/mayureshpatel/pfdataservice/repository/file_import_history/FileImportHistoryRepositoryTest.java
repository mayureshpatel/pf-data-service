package com.mayureshpatel.pfdataservice.repository.file_import_history;

import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.dto.transaction.fileimport.FileImportCreateRequest;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
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
            // Arrange
            FileImportCreateRequest request = FileImportCreateRequest.builder()
                    .accountId(String.valueOf(ACCOUNT_1))
                    .fileHash("hash123")
                    .fileName("test.csv")
                    .build();

            // Act
            int rows = repository.insert(request);
            Optional<FileImportHistory> result = repository.findByAccountIdAndFileHash(ACCOUNT_1, "hash123");

            // Assert
            assertEquals(1, rows);
            assertTrue(result.isPresent());
            assertEquals("test.csv", result.get().getFileName());
        }

        @Test
        @DisplayName("should find by file hash")
        void shouldFindByFileHash() {
            // Arrange
            FileImportCreateRequest request = FileImportCreateRequest.builder()
                    .accountId(String.valueOf(ACCOUNT_1))
                    .fileHash("hash456")
                    .fileName("test2.csv")
                    .build();
            repository.insert(request);

            // Act
            Optional<FileImportHistory> result = repository.findByFileHash("hash456");

            // Assert
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("should find all by account ID")
        void shouldFindAllByAccountId() {
            // Arrange
            repository.insert(FileImportCreateRequest.builder().accountId(String.valueOf(ACCOUNT_1)).fileHash("h1").fileName("1.csv").build());
            repository.insert(FileImportCreateRequest.builder().accountId(String.valueOf(ACCOUNT_1)).fileHash("h2").fileName("2.csv").build());

            // Act
            List<FileImportHistory> result = repository.findAllByAccountId(ACCOUNT_1);

            // Assert
            assertTrue(result.size() >= 2);
        }

        @Test
        @DisplayName("should find by ID")
        void shouldFindById() {
            // Arrange
            repository.insert(FileImportCreateRequest.builder().accountId(String.valueOf(ACCOUNT_1)).fileHash("h3").fileName("3.csv").build());
            Long id = repository.findByFileHash("h3").get().getId();

            // Act
            Optional<FileImportHistory> result = repository.findById(id);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(id, result.get().getId());
        }

        @Test
        @DisplayName("should delete by ID")
        void shouldDeleteById() {
            // Arrange
            repository.insert(FileImportCreateRequest.builder().accountId(String.valueOf(ACCOUNT_1)).fileHash("h4").fileName("4.csv").build());
            Long id = repository.findByFileHash("h4").get().getId();

            // Act
            int rows = repository.deleteById(id);

            // Assert
            assertEquals(1, rows);
            assertTrue(repository.findById(id).isEmpty());
        }
    }
}
