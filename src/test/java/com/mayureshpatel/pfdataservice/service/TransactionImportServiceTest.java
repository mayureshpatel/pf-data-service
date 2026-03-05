package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionPreviewDto;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import com.mayureshpatel.pfdataservice.exception.DuplicateImportException;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.file_import_history.FileImportHistoryRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParser;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParserFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionImportService Unit Tests")
class TransactionImportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private FileImportHistoryRepository fileImportHistoryRepository;
    @Mock
    private TransactionParserFactory parserFactory;
    @Mock
    private TransactionCategorizer categorizer;
    @Mock
    private CategoryRuleRepository categoryRuleRepository;

    @InjectMocks
    private TransactionImportService importService;

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;

    @Nested
    @DisplayName("previewTransactions")
    class PreviewTransactionsTests {
        @Test
        @DisplayName("should parse and return previews with suggested categories")
        void shouldReturnPreviews() {
            // Arrange
            String bankName = "Standard";
            InputStream stream = new ByteArrayInputStream("test".getBytes());
            TransactionParser parser = mock(TransactionParser.class);
            Transaction t = Transaction.builder().description("Test").amount(BigDecimal.TEN).transactionDate(OffsetDateTime.now()).build();
            Category cat = Category.builder().id(5L).name("Food").build();

            when(parserFactory.getTransactionParser(bankName)).thenReturn(parser);
            when(parser.parse(eq(ACCOUNT_ID), any())).thenReturn(Stream.of(t));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(cat));
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(5L);

            // Act
            List<TransactionPreviewDto> result = importService.previewTransactions(USER_ID, ACCOUNT_ID, bankName, stream, "test.csv");

            // Assert
            assertEquals(1, result.size());
            assertEquals("Food", result.get(0).suggestedCategory().name());
        }

        @Test
        @DisplayName("should handle category guess <= 0")
        void shouldHandleNoCategoryGuess() {
            // Arrange
            String bankName = "Standard";
            TransactionParser parser = mock(TransactionParser.class);
            Transaction t = Transaction.builder().transactionDate(OffsetDateTime.now()).amount(BigDecimal.TEN).build();
            when(parserFactory.getTransactionParser(bankName)).thenReturn(parser);
            when(parser.parse(anyLong(), any())).thenReturn(Stream.of(t));
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(0L);

            // Act
            List<TransactionPreviewDto> result = importService.previewTransactions(USER_ID, ACCOUNT_ID, bankName, new ByteArrayInputStream(new byte[0]), "test.csv");

            // Assert
            assertNull(result.get(0).suggestedCategory());
        }

        @Test
        @DisplayName("should throw CsvParsingException if parsing fails")
        void shouldThrowOnException() {
            // Arrange
            String bankName = "Standard";
            TransactionParser parser = mock(TransactionParser.class);
            when(parserFactory.getTransactionParser(bankName)).thenReturn(parser);
            when(parser.parse(anyLong(), any())).thenThrow(new RuntimeException("Oops"));

            // Act & Assert
            assertThrows(CsvParsingException.class, () -> importService.previewTransactions(USER_ID, ACCOUNT_ID, bankName, null, "test.csv"));
        }
    }

    @Nested
    @DisplayName("saveTransactions")
    class SaveTransactionsTests {
        @Test
        @DisplayName("should save unique transactions and update account balance")
        void shouldSaveAndBalance() {
            // Arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(BigDecimal.ZERO).build();
            TransactionDto dto = TransactionDto.builder().description("Test").amount(BigDecimal.TEN).date(OffsetDateTime.now()).type(TransactionType.INCOME).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(fileImportHistoryRepository.findByAccountIdAndFileHash(anyLong(), anyString())).thenReturn(Optional.empty());
            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(anyLong(), any(), any(), anyString(), any())).thenReturn(false);

            // Act
            int result = importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), "file.csv", "hash");

            // Assert
            assertEquals(1, result);
            verify(transactionRepository).insertAll(anyList());
            verify(accountRepository).update(any(Account.class));
            verify(fileImportHistoryRepository).save(any(FileImportHistory.class));
        }

        @Test
        @DisplayName("should throw DuplicateImportException if file hash exists")
        void shouldThrowOnDuplicateHash() {
            // Arrange
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(Account.builder().build()));
            when(fileImportHistoryRepository.findByAccountIdAndFileHash(anyLong(), eq("existing"))).thenReturn(Optional.of(FileImportHistory.builder().build()));

            // Act & Assert
            assertThrows(DuplicateImportException.class, () -> importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(), "file.csv", "existing"));
        }

        @Test
        @DisplayName("should skip duplicate transactions within batch and database")
        void shouldSkipDuplicates() {
            // Arrange
            Account account = Account.builder().id(ACCOUNT_ID).currentBalance(BigDecimal.ZERO).build();
            TransactionDto dto1 = TransactionDto.builder().description("D1").amount(BigDecimal.TEN).date(OffsetDateTime.now()).type(TransactionType.INCOME).build();
            TransactionDto dto2 = TransactionDto.builder().description("D1").amount(BigDecimal.TEN).date(dto1.date()).type(TransactionType.INCOME).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(eq(ACCOUNT_ID), eq(dto1.date()), eq(BigDecimal.TEN), eq("D1"), eq(TransactionType.INCOME))).thenReturn(false);

            // Act
            int result = importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto1, dto2), null, null);

            // Assert
            assertEquals(1, result);
            verify(transactionRepository).insertAll(argThat(list -> list.size() == 1));
        }

        @Test
        @DisplayName("should handle empty approved list")
        void shouldHandleEmptyList() {
            // Arrange
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(Account.builder().build()));

            // Act
            int result = importService.saveTransactions(USER_ID, ACCOUNT_ID, Collections.emptyList(), null, null);

            // Assert
            assertEquals(0, result);
            verify(transactionRepository, never()).insertAll(any());
        }

        @Test
        @DisplayName("should skip if transaction exists in database")
        void shouldSkipOnDatabaseMatch() {
            // Arrange
            Account account = Account.builder().id(ACCOUNT_ID).currentBalance(BigDecimal.ZERO).build();
            TransactionDto dto = TransactionDto.builder().description("D1").amount(BigDecimal.TEN).date(OffsetDateTime.now()).type(TransactionType.INCOME).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(anyLong(), any(), any(), anyString(), any())).thenReturn(true);

            // Act
            int result = importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), null, null);

            // Assert
            assertEquals(0, result);
            verify(transactionRepository, never()).insertAll(any());
        }

        @Test
        @DisplayName("should skip history if fileName is null")
        void shouldSkipHistoryIfFileNameNull() {
            // Arrange
            Account account = Account.builder().id(ACCOUNT_ID).build();
            TransactionDto dto = TransactionDto.builder().description("T").amount(BigDecimal.TEN).date(OffsetDateTime.now()).type(TransactionType.INCOME).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(anyLong(), any(), any(), anyString(), any())).thenReturn(false);

            // Act
            importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), null, "hash");

            // Assert
            verify(fileImportHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip history if fileHash is null")
        void shouldSkipHistoryIfFileHashNull() {
            // Arrange
            Account account = Account.builder().id(ACCOUNT_ID).build();
            TransactionDto dto = TransactionDto.builder().description("T").amount(BigDecimal.TEN).date(OffsetDateTime.now()).type(TransactionType.INCOME).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(anyLong(), any(), any(), anyString(), any())).thenReturn(false);

            // Act
            importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), "file.csv", null);

            // Assert
            verify(fileImportHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if account missing")
        void shouldThrowOnMissingAccount() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(), null, null));
        }
    }

    @Nested
    @DisplayName("calculateFileHash")
    class HashTests {
        @Test
        @DisplayName("should calculate MD5 hex string")
        void shouldHash() throws IOException {
            InputStream stream = new ByteArrayInputStream("content".getBytes());
            String hash = importService.calculateFileHash(stream);
            assertNotNull(hash);
            assertEquals(32, hash.length());
        }

        @Test
        @DisplayName("should hash byte array")
        void shouldHashBytes() {
            String hash = importService.calculateFileHash("content".getBytes());
            assertNotNull(hash);
        }
    }
}
