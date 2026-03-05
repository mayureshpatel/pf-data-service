//package com.mayureshpatel.pfdataservice.service;
//
//import com.mayureshpatel.pfdataservice.domain.account.Account;
//import com.mayureshpatel.pfdataservice.domain.category.Category;
//import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
//import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
//import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
//import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
//import com.mayureshpatel.pfdataservice.dto.transaction.TransactionPreviewDto;
//import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
//import com.mayureshpatel.pfdataservice.exception.DuplicateImportException;
//import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
//import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
//import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
//import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
//import com.mayureshpatel.pfdataservice.repository.file_import_history.FileImportHistoryRepository;
//import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
//import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
//import com.mayureshpatel.pfdataservice.service.parser.TransactionParser;
//import com.mayureshpatel.pfdataservice.service.parser.TransactionParserFactory;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.io.ByteArrayInputStream;
//import java.io.InputStream;
//import java.math.BigDecimal;
//import java.nio.charset.StandardCharsets;
//import java.time.OffsetDateTime;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Stream;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("TransactionImportService unit tests")
//class TransactionImportServiceTest {
//
//    @Mock
//    private TransactionRepository transactionRepository;
//    @Mock
//    private AccountRepository accountRepository;
//    @Mock
//    private CategoryRepository categoryRepository;
//    @Mock
//    private FileImportHistoryRepository fileImportHistoryRepository;
//    @Mock
//    private TransactionParserFactory parserFactory;
//    @Mock
//    private TransactionCategorizer categorizer;
//    @Mock
//    private CategoryRuleRepository categoryRuleRepository;
//    @Mock
//    private TransactionParser mockParser;
//
//    @InjectMocks
//    private TransactionImportService service;
//
//    private static final Long USER_ID = 1L;
//    private static final Long ACCOUNT_ID = 10L;
//    private static final String BANK_NAME = "STANDARD";
//    private static final String FILE_NAME = "transactions.csv";
//    private static final String FILE_HASH = "abc123hash";
//
//    private InputStream emptyStream() {
//        return new ByteArrayInputStream(new byte[0]);
//    }
//
//    private Account buildAccount(Long id, BigDecimal balance) {
//        Account account = new Account();
//        account.setId(id);
//        account.setCurrentBalance(balance);
//        return account;
//    }
//
//    private Transaction buildTransaction() {
//        Transaction t = new Transaction();
//        t.setDescription("Coffee");
//        t.setAmount(new BigDecimal("10.00"));
//        t.setType(TransactionType.EXPENSE);
//        t.setTransactionDate(OffsetDateTime.now());
//        return t;
//    }
//
//    private TransactionDto buildDto() {
//        return TransactionDto.builder()
//                .date(OffsetDateTime.now())
//                .description("Coffee Shop")
//                .amount(new BigDecimal("25.00"))
//                .type(TransactionType.EXPENSE)
//                .build();
//    }
//
//    @Nested
//    @DisplayName("previewTransactions()")
//    class PreviewTransactionsTests {
//
//        @Test
//        @DisplayName("should return a preview for each parsed transaction when categorizer returns -1")
//        void previewTransactions_categorizerReturnsMinusOne_previewHasNullSuggestedCategory() {
//            Transaction t = buildTransaction();
//
//            when(parserFactory.getTransactionParser(BANK_NAME)).thenReturn(mockParser);
//            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
//            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());
//            when(mockParser.parse(eq(ACCOUNT_ID), any())).thenReturn(Stream.of(t));
//            when(categorizer.guessCategory(any(Transaction.class), any(), any())).thenReturn(-1L);
//
//            List<TransactionPreviewDto> result = service.previewTransactions(
//                    USER_ID, ACCOUNT_ID, BANK_NAME, emptyStream(), FILE_NAME);
//
//            assertThat(result).hasSize(1);
//            assertThat(result.get(0).suggestedCategory()).isNull();
//            assertThat(result.get(0).description()).isEqualTo("Coffee");
//            assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("10.00"));
//            assertThat(result.get(0).type()).isEqualTo(TransactionType.EXPENSE);
//        }
//
//        @Test
//        @DisplayName("should populate suggestedCategory when categorizer returns a valid category ID")
//        void previewTransactions_categorizerMatchesCategory_previewHasSuggestedCategory() {
//            Transaction t = buildTransaction();
//            Category category = new Category();
//            category.setId(5L);
//            category.setName("Food & Dining");
//
//            when(parserFactory.getTransactionParser(BANK_NAME)).thenReturn(mockParser);
//            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
//            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(category));
//            when(mockParser.parse(eq(ACCOUNT_ID), any())).thenReturn(Stream.of(t));
//            when(categorizer.guessCategory(any(Transaction.class), any(), any())).thenReturn(5L);
//
//            List<TransactionPreviewDto> result = service.previewTransactions(
//                    USER_ID, ACCOUNT_ID, BANK_NAME, emptyStream(), FILE_NAME);
//
//            assertThat(result).hasSize(1);
//            assertThat(result.get(0).suggestedCategory()).isNotNull();
//            assertThat(result.get(0).suggestedCategory().name()).isEqualTo("Food & Dining");
//        }
//
//        @Test
//        @DisplayName("should return empty list when parser returns no transactions")
//        void previewTransactions_emptyStream_returnsEmptyList() {
//            when(parserFactory.getTransactionParser(BANK_NAME)).thenReturn(mockParser);
//            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
//            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());
//            when(mockParser.parse(eq(ACCOUNT_ID), any())).thenReturn(Stream.empty());
//
//            List<TransactionPreviewDto> result = service.previewTransactions(
//                    USER_ID, ACCOUNT_ID, BANK_NAME, emptyStream(), FILE_NAME);
//
//            assertThat(result).isEmpty();
//        }
//
//        @Test
//        @DisplayName("should wrap parser exception in CsvParsingException with correct message")
//        void previewTransactions_parserThrows_throwsCsvParsingException() {
//            when(parserFactory.getTransactionParser(BANK_NAME)).thenReturn(mockParser);
//            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
//            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());
//            when(mockParser.parse(eq(ACCOUNT_ID), any()))
//                    .thenThrow(new RuntimeException("CSV parse error"));
//
//            assertThatThrownBy(() -> service.previewTransactions(
//                    USER_ID, ACCOUNT_ID, BANK_NAME, emptyStream(), FILE_NAME))
//                    .isInstanceOf(CsvParsingException.class)
//                    .hasMessageContaining("Error processing transaction file");
//        }
//    }
//
//    @Nested
//    @DisplayName("saveTransactions() — account lookup")
//    class AccountLookupTests {
//
//        @Test
//        @DisplayName("should throw ResourceNotFoundException when account is not found")
//        void saveTransactions_accountNotFound_throwsResourceNotFoundException() {
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> service.saveTransactions(
//                    USER_ID, ACCOUNT_ID, List.of(buildDto()), FILE_NAME, FILE_HASH))
//                    .isInstanceOf(ResourceNotFoundException.class);
//
//            verify(transactionRepository, never()).saveAll(anyList());
//        }
//    }
//
//    @Nested
//    @DisplayName("saveTransactions() — duplicate detection")
//    class DuplicateDetectionTests {
//
//        @Test
//        @DisplayName("should throw DuplicateImportException when file hash already exists")
//        void saveTransactions_duplicateFileHash_throwsDuplicateImportException() {
//            Account account = buildAccount(ACCOUNT_ID, new BigDecimal("1000.00"));
//            FileImportHistory existing = new FileImportHistory();
//
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
//            when(fileImportHistoryRepository.findByAccountIdAndFileHash(ACCOUNT_ID, FILE_HASH))
//                    .thenReturn(Optional.of(existing));
//
//            assertThatThrownBy(() -> service.saveTransactions(
//                    USER_ID, ACCOUNT_ID, List.of(buildDto()), FILE_NAME, FILE_HASH))
//                    .isInstanceOf(DuplicateImportException.class)
//                    .hasMessageContaining("already been imported");
//
//            verify(transactionRepository, never()).saveAll(anyList());
//        }
//
//        @Test
//        @DisplayName("should skip duplicate check when fileHash is null")
//        void saveTransactions_nullFileHash_skipsDuplicateCheck() {
//            Account account = buildAccount(ACCOUNT_ID, new BigDecimal("1000.00"));
//            TransactionDto dto = buildDto();
//
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
//            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
//                    eq(ACCOUNT_ID), any(), any(), any(), any())).thenReturn(false);
//            when(transactionRepository.saveAll(anyList())).thenReturn(List.of());
//            when(accountRepository.save(any(Account.class))).thenReturn(account);
//
//            int saved = service.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), FILE_NAME, null);
//
//            assertThat(saved).isEqualTo(1);
//            verify(fileImportHistoryRepository, never()).findByAccountIdAndFileHash(any(), any());
//            // No file history saved because fileHash is null
//            verify(fileImportHistoryRepository, never()).save(any());
//        }
//    }
//
//    @Nested
//    @DisplayName("saveTransactions() — transaction deduplication")
//    class TransactionDeduplicationTests {
//
//        @Test
//        @DisplayName("should save all transactions when none are duplicates and return correct count")
//        void saveTransactions_noduplicates_savesAllAndReturnsCount() {
//            Account account = buildAccount(ACCOUNT_ID, new BigDecimal("1000.00"));
//            TransactionDto dto = buildDto();
//
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
//            when(fileImportHistoryRepository.findByAccountIdAndFileHash(ACCOUNT_ID, FILE_HASH))
//                    .thenReturn(Optional.empty());
//            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
//                    eq(ACCOUNT_ID), any(), any(), any(), any())).thenReturn(false);
//            when(transactionRepository.saveAll(anyList())).thenReturn(List.of());
//            when(accountRepository.save(any(Account.class))).thenReturn(account);
//            when(fileImportHistoryRepository.save(any(FileImportHistory.class))).thenReturn(new FileImportHistory());
//
//            int result = service.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), FILE_NAME, FILE_HASH);
//
//            assertThat(result).isEqualTo(1);
//            verify(transactionRepository).saveAll(anyList());
//            verify(accountRepository).save(any(Account.class));
//            verify(fileImportHistoryRepository).save(any(FileImportHistory.class));
//        }
//
//        @Test
//        @DisplayName("should skip duplicate transactions and return only the count of unique ones saved")
//        void saveTransactions_withDuplicates_skipsThemAndReturnsUniqueCount() {
//            Account account = buildAccount(ACCOUNT_ID, new BigDecimal("1000.00"));
//            TransactionDto dto1 = buildDto();
//            TransactionDto dto2 = TransactionDto.builder()
//                    .date(OffsetDateTime.now().minusDays(1))
//                    .description("Different transaction")
//                    .amount(new BigDecimal("50.00"))
//                    .type(TransactionType.INCOME)
//                    .build();
//
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
//            when(fileImportHistoryRepository.findByAccountIdAndFileHash(ACCOUNT_ID, FILE_HASH))
//                    .thenReturn(Optional.empty());
//            // dto1 is duplicate, dto2 is unique
//            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
//                    eq(ACCOUNT_ID), eq(dto1.date()), any(), eq(dto1.description()), eq(dto1.type())))
//                    .thenReturn(true);
//            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
//                    eq(ACCOUNT_ID), eq(dto2.date()), any(), eq(dto2.description()), eq(dto2.type())))
//                    .thenReturn(false);
//            when(transactionRepository.saveAll(anyList())).thenReturn(List.of());
//            when(accountRepository.save(any(Account.class))).thenReturn(account);
//            when(fileImportHistoryRepository.save(any())).thenReturn(new FileImportHistory());
//
//            int result = service.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto1, dto2), FILE_NAME, FILE_HASH);
//
//            assertThat(result).isEqualTo(1);
//        }
//
//        @Test
//        @DisplayName("should return 0 and not call saveAll when all transactions are duplicates")
//        void saveTransactions_allDuplicates_returnsZeroAndDoesNotSave() {
//            Account account = buildAccount(ACCOUNT_ID, new BigDecimal("1000.00"));
//            TransactionDto dto = buildDto();
//
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
//            when(fileImportHistoryRepository.findByAccountIdAndFileHash(ACCOUNT_ID, FILE_HASH))
//                    .thenReturn(Optional.empty());
//            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
//                    eq(ACCOUNT_ID), any(), any(), any(), any())).thenReturn(true);
//
//            int result = service.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), FILE_NAME, FILE_HASH);
//
//            assertThat(result).isEqualTo(0);
//            verify(transactionRepository, never()).saveAll(anyList());
//            verify(accountRepository, never()).save(any());
//        }
//    }
//
//    @Nested
//    @DisplayName("saveTransactions() — file import history")
//    class FileImportHistoryTests {
//
//        @Test
//        @DisplayName("should save file import history when fileName and fileHash are non-null")
//        void saveTransactions_withFileNameAndHash_savesImportHistory() {
//            Account account = buildAccount(ACCOUNT_ID, new BigDecimal("1000.00"));
//            TransactionDto dto = buildDto();
//
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
//            when(fileImportHistoryRepository.findByAccountIdAndFileHash(ACCOUNT_ID, FILE_HASH))
//                    .thenReturn(Optional.empty());
//            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
//                    eq(ACCOUNT_ID), any(), any(), any(), any())).thenReturn(false);
//            when(transactionRepository.saveAll(anyList())).thenReturn(List.of());
//            when(accountRepository.save(any())).thenReturn(account);
//            when(fileImportHistoryRepository.save(any(FileImportHistory.class))).thenReturn(new FileImportHistory());
//
//            service.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), FILE_NAME, FILE_HASH);
//
//            ArgumentCaptor<FileImportHistory> historyCaptor = ArgumentCaptor.forClass(FileImportHistory.class);
//            verify(fileImportHistoryRepository).save(historyCaptor.capture());
//
//            FileImportHistory savedHistory = historyCaptor.getValue();
//            assertThat(savedHistory.getFileName()).isEqualTo(FILE_NAME);
//            assertThat(savedHistory.getFileHash()).isEqualTo(FILE_HASH);
//            assertThat(savedHistory.getTransactionCount()).isEqualTo(1);
//        }
//
//        @Test
//        @DisplayName("should not save file import history when fileName is null")
//        void saveTransactions_nullFileName_doesNotSaveImportHistory() {
//            Account account = buildAccount(ACCOUNT_ID, new BigDecimal("1000.00"));
//            TransactionDto dto = buildDto();
//
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
//            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
//                    eq(ACCOUNT_ID), any(), any(), any(), any())).thenReturn(false);
//            when(transactionRepository.saveAll(anyList())).thenReturn(List.of());
//            when(accountRepository.save(any())).thenReturn(account);
//
//            service.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), null, FILE_HASH);
//
//            verify(fileImportHistoryRepository, never()).save(any());
//        }
//    }
//
//    @Nested
//    @DisplayName("saveTransactions() — account balance update")
//    class AccountBalanceTests {
//
//        @Test
//        @DisplayName("should decrease account balance when saving EXPENSE transaction")
//        void saveTransactions_expenseTransaction_decreasesAccountBalance() {
//            Account account = buildAccount(ACCOUNT_ID, new BigDecimal("1000.00"));
//            TransactionDto dto = TransactionDto.builder()
//                    .date(OffsetDateTime.now())
//                    .description("Expense")
//                    .amount(new BigDecimal("100.00"))
//                    .type(TransactionType.EXPENSE)
//                    .build();
//
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
//            when(fileImportHistoryRepository.findByAccountIdAndFileHash(ACCOUNT_ID, FILE_HASH))
//                    .thenReturn(Optional.empty());
//            when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
//                    eq(ACCOUNT_ID), any(), any(), any(), any())).thenReturn(false);
//            when(transactionRepository.saveAll(anyList())).thenReturn(List.of());
//            when(accountRepository.save(any(Account.class))).thenReturn(account);
//            when(fileImportHistoryRepository.save(any())).thenReturn(new FileImportHistory());
//
//            service.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), FILE_NAME, FILE_HASH);
//
//            // EXPENSE net change = -amount → balance should decrease by 100
//            assertThat(account.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("900.00"));
//        }
//    }
//
//    @Nested
//    @DisplayName("calculateFileHash()")
//    class CalculateFileHashTests {
//
//        @Test
//        @DisplayName("should return MD5 hex string for byte array input")
//        void calculateFileHash_byteArray_returnsMd5Hex() {
//            byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
//
//            String hash = service.calculateFileHash(content);
//
//            // Known MD5 of "hello"
//            assertThat(hash).isEqualTo("5d41402abc4b2a76b9719d911017c592");
//        }
//
//        @Test
//        @DisplayName("should return MD5 hex string for empty byte array")
//        void calculateFileHash_emptyByteArray_returnsEmptyStringMd5() {
//            byte[] content = new byte[0];
//
//            String hash = service.calculateFileHash(content);
//
//            // Known MD5 of empty string
//            assertThat(hash).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
//        }
//
//        @Test
//        @DisplayName("should return MD5 hex string for InputStream input")
//        void calculateFileHash_inputStream_returnsMd5Hex() throws Exception {
//            InputStream is = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));
//
//            String hash = service.calculateFileHash(is);
//
//            assertThat(hash).isEqualTo("5d41402abc4b2a76b9719d911017c592");
//        }
//
//        @Test
//        @DisplayName("should return consistent hash for the same content via both overloads")
//        void calculateFileHash_sameContent_sameHashViaBothOverloads() throws Exception {
//            byte[] content = "test content".getBytes(StandardCharsets.UTF_8);
//            InputStream is = new ByteArrayInputStream(content.clone());
//
//            String hashFromBytes = service.calculateFileHash(content);
//            String hashFromStream = service.calculateFileHash(is);
//
//            assertThat(hashFromBytes).isEqualTo(hashFromStream);
//        }
//    }
//}
