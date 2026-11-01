package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.FileImportResultDto;
import com.mayureshpatel.pfdataservice.dto.transaction.FileImportStatus;
import com.mayureshpatel.pfdataservice.dto.transaction.ImportHashCheckRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
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
import org.springframework.security.access.AccessDeniedException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Verifies {@code TransactionImportService}'s two-phase preview/save workflow, one {@code @Nested} class per phase below. */
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
    @Mock
    private MerchantService merchantService;
    @Mock
    private AccountBalanceUpdateService accountBalanceUpdateService;
    @Mock
    private TransactionImportService self;

    @InjectMocks
    private TransactionImportService importService;

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final String BANK_STANDARD = "Standard";
    private static final String DESCRIPTION_TEST = "Test";
    private static final String TEST_CSV_FILENAME = "test.csv";
    private static final String IMPORT_FILENAME = "file.csv";

    /**
     * PF-854: mimics the real service's success path (apply the transform once, return the
     * result). Not a blanket {@code @BeforeEach} default, matching this file's existing
     * per-test-explicit stubbing style -- only tests that actually save transactions need it.
     */
    private void stubBalanceUpdateSuccess() {
        when(accountBalanceUpdateService.applyWithRetry(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Account account = invocation.getArgument(1);
                    UnaryOperator<Account> transform = invocation.getArgument(2);
                    return transform.apply(account);
                });
    }

    /**
     * {@code previewTransactions} parses the uploaded file, attaches a suggested category per row
     * from {@code TransactionCategorizer}, and treats a null, zero, or unrecognized-id category
     * guess all the same way: no suggestion rather than an error. A parsing failure is wrapped as
     * {@link CsvParsingException}; account-not-found and not-the-owner are checked first, before
     * any parsing is attempted.
     */
    @Nested
    @DisplayName("previewTransactions")
    class PreviewTransactionsTests {
        @Test
        @DisplayName("should parse and return previews with suggested categories")
        void shouldReturnPreviews() {
            // arrange
            String bankName = BANK_STANDARD;
            InputStream stream = new ByteArrayInputStream("test".getBytes());
            TransactionParser parser = mock(TransactionParser.class);
            Transaction t = Transaction.builder().description(DESCRIPTION_TEST).amount(BigDecimal.TEN).transactionDate(OffsetDateTime.now()).build();
            Category cat = Category.builder().id(5L).name("Food").parentId(1L)
                    .parent(Category.builder().id(1L).userId(USER_ID).name("Everyday").build()).build();
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(parserFactory.getTransactionParser(bankName)).thenReturn(parser);
            when(parser.parse(eq(ACCOUNT_ID), any())).thenReturn(Stream.of(t));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(cat));
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(5L);

            // act
            List<TransactionPreviewDto> result = importService.previewTransactions(USER_ID, ACCOUNT_ID, bankName, stream, TEST_CSV_FILENAME);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("Food", result.get(0).suggestedCategory().name());
        }

        @Test
        @DisplayName("previewTransactions_guessIsTopLevelCategory_suggestsNoCategory")
        void previewTransactions_guessIsTopLevelCategory_suggestsNoCategory() {
            // arrange
            TransactionParser parser = mock(TransactionParser.class);
            Transaction t = Transaction.builder().description(DESCRIPTION_TEST).amount(BigDecimal.TEN).transactionDate(OffsetDateTime.now()).build();
            Category parent = Category.builder().id(1L).userId(USER_ID).name("Food").build();
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(parserFactory.getTransactionParser(BANK_STANDARD)).thenReturn(parser);
            when(parser.parse(eq(ACCOUNT_ID), any())).thenReturn(Stream.of(t));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(parent));
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(1L);

            // act
            List<TransactionPreviewDto> result = importService.previewTransactions(USER_ID, ACCOUNT_ID, BANK_STANDARD,
                    new ByteArrayInputStream(new byte[0]), TEST_CSV_FILENAME);

            // assert & verify
            assertNull(result.get(0).suggestedCategory());
        }

        @Test
        @DisplayName("should handle category guess null or <= 0")
        void shouldHandleNoCategoryGuess() {
            // arrange
            String bankName = BANK_STANDARD;
            TransactionParser parser = mock(TransactionParser.class);
            Transaction t1 = Transaction.builder().transactionDate(OffsetDateTime.now()).amount(BigDecimal.TEN).build();
            Transaction t2 = Transaction.builder().transactionDate(OffsetDateTime.now()).amount(BigDecimal.ONE).build();
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(parserFactory.getTransactionParser(bankName)).thenReturn(parser);
            // Return a fresh stream for each call to avoid IllegalStateException
            when(parser.parse(anyLong(), any())).thenReturn(Stream.of(t1), Stream.of(t2));

            // Case 1: categoryId is null
            when(categorizer.guessCategory(eq(t1), anyList(), anyList())).thenReturn(null);
            List<TransactionPreviewDto> result = importService.previewTransactions(USER_ID, ACCOUNT_ID, bankName, new ByteArrayInputStream(new byte[0]), TEST_CSV_FILENAME);
            assertNull(result.get(0).suggestedCategory());

            // Case 2: categoryId is 0
            when(categorizer.guessCategory(eq(t2), anyList(), anyList())).thenReturn(0L);
            result = importService.previewTransactions(USER_ID, ACCOUNT_ID, bankName, new ByteArrayInputStream(new byte[0]), TEST_CSV_FILENAME);
            assertNull(result.get(0).suggestedCategory());
        }

        @Test
        @DisplayName("should handle category guessed ID not in list")
        void shouldHandleGuessedCategoryNotFound() {
            // arrange
            String bankName = BANK_STANDARD;
            TransactionParser parser = mock(TransactionParser.class);
            Transaction t = Transaction.builder().transactionDate(OffsetDateTime.now()).amount(BigDecimal.TEN).build();
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(parserFactory.getTransactionParser(bankName)).thenReturn(parser);
            when(parser.parse(anyLong(), any())).thenReturn(Stream.of(t));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(99L);

            // act
            List<TransactionPreviewDto> result = importService.previewTransactions(USER_ID, ACCOUNT_ID, bankName, new ByteArrayInputStream(new byte[0]), TEST_CSV_FILENAME);

            // assert & verify
            assertNull(result.get(0).suggestedCategory());
        }

        @Test
        @DisplayName("should throw CsvParsingException if parsing fails")
        void shouldThrowOnException() {
            // arrange
            String bankName = BANK_STANDARD;
            TransactionParser parser = mock(TransactionParser.class);
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(parserFactory.getTransactionParser(bankName)).thenReturn(parser);
            // Throw inside the try block (during parse or stream processing)
            when(parser.parse(anyLong(), any())).thenThrow(new RuntimeException("Oops"));

            // act & assert & verify
            assertThrows(CsvParsingException.class, () -> importService.previewTransactions(USER_ID, ACCOUNT_ID, bankName, null, TEST_CSV_FILENAME));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own account during preview")
        void shouldThrowOnAccessDenied() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(99L).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> importService.previewTransactions(USER_ID, ACCOUNT_ID, BANK_STANDARD, null, TEST_CSV_FILENAME));
        }

        /** Stubs account ownership, parser lookup and the parsed rows shared by the PF-898 preview tests. */
        private void stubParsedRows(Transaction... rows) {
            TransactionParser parser = mock(TransactionParser.class);
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(parserFactory.getTransactionParser(BANK_STANDARD)).thenReturn(parser);
            when(parser.parse(eq(ACCOUNT_ID), any())).thenReturn(Stream.of(rows));
        }

        private Transaction row(String description, OffsetDateTime date, String amount) {
            return Transaction.builder().description(description).amount(new BigDecimal(amount))
                    .transactionDate(date).type(TransactionType.EXPENSE).build();
        }

        @Test
        @DisplayName("previewTransactions_descriptionWithLinkedMerchant_setsSuggestedMerchant")
        void previewTransactions_descriptionWithLinkedMerchant_setsSuggestedMerchant() {
            // arrange
            stubParsedRows(row("STARBUCKS #1", OffsetDateTime.now(), "5.00"));
            Merchant merchant = Merchant.builder().id(7L).userId(USER_ID).name("Starbucks").build();
            when(merchantService.findMatchingMerchants(eq(USER_ID), anyCollection())).thenReturn(Map.of("STARBUCKS #1", merchant));

            // act
            List<TransactionPreviewDto> result = importService.previewTransactions(USER_ID, ACCOUNT_ID, BANK_STANDARD, new ByteArrayInputStream(new byte[0]), TEST_CSV_FILENAME);

            // assert & verify
            assertEquals(7L, result.get(0).suggestedMerchant().id());
            assertEquals("Starbucks", result.get(0).suggestedMerchant().name());
            verify(merchantService, never()).createMerchant(any(), any());
            verify(merchantService, never()).recordDescriptionLink(any(), any(), any());
        }

        @Test
        @DisplayName("previewTransactions_unlinkedDescription_leavesMerchantNull")
        void previewTransactions_unlinkedDescription_leavesMerchantNull() {
            // arrange
            stubParsedRows(row("UNKNOWN SHOP", OffsetDateTime.now(), "5.00"));
            when(merchantService.findMatchingMerchants(eq(USER_ID), anyCollection())).thenReturn(Map.of());

            // act
            List<TransactionPreviewDto> result = importService.previewTransactions(USER_ID, ACCOUNT_ID, BANK_STANDARD, new ByteArrayInputStream(new byte[0]), TEST_CSV_FILENAME);

            // assert & verify
            assertNull(result.get(0).suggestedMerchant());
        }

        @Test
        @DisplayName("previewTransactions_rowMatchingSavedTransaction_isFlaggedDuplicate")
        void previewTransactions_rowMatchingSavedTransaction_isFlaggedDuplicate() {
            // arrange
            OffsetDateTime date = OffsetDateTime.parse("2026-10-01T00:00:00Z");
            stubParsedRows(row("COFFEE", date, "5.00"), row("LUNCH", date, "9.00"));
            // the saved amount has different trailing zeros: the signature must still match
            when(transactionRepository.findExistingForDuplicateCheck(eq(ACCOUNT_ID), any(), any()))
                    .thenReturn(List.of(row("COFFEE", date, "5.0")));

            // act
            List<TransactionPreviewDto> result = importService.previewTransactions(USER_ID, ACCOUNT_ID, BANK_STANDARD, new ByteArrayInputStream(new byte[0]), TEST_CSV_FILENAME);

            // assert & verify
            assertTrue(result.get(0).duplicate());
            assertFalse(result.get(1).duplicate());
        }

        @Test
        @DisplayName("previewTransactions_repeatedRowInSameFile_secondIsFlaggedDuplicate")
        void previewTransactions_repeatedRowInSameFile_secondIsFlaggedDuplicate() {
            // arrange
            OffsetDateTime date = OffsetDateTime.parse("2026-10-01T00:00:00Z");
            stubParsedRows(row("COFFEE", date, "5.00"), row("COFFEE", date, "5.00"));

            // act
            List<TransactionPreviewDto> result = importService.previewTransactions(USER_ID, ACCOUNT_ID, BANK_STANDARD, new ByteArrayInputStream(new byte[0]), TEST_CSV_FILENAME);

            // assert & verify
            assertFalse(result.get(0).duplicate());
            assertTrue(result.get(1).duplicate());
        }

        @Test
        @DisplayName("previewTransactions_headerOnlyFile_returnsEmptyList")
        void previewTransactions_headerOnlyFile_returnsEmptyList() {
            // arrange
            stubParsedRows();

            // act
            List<TransactionPreviewDto> result = importService.previewTransactions(USER_ID, ACCOUNT_ID, BANK_STANDARD, new ByteArrayInputStream(new byte[0]), TEST_CSV_FILENAME);

            // assert & verify
            assertTrue(result.isEmpty());
            verifyNoInteractions(merchantService);
        }
    }

    /**
     * {@code saveTransactions} skips rows that already exist (by file-hash-level or
     * individual-transaction-level duplicate detection, the latter deduplicating within the batch
     * itself too), applies the account balance update via {@code accountBalanceUpdateService}
     * (stubbed here through {@link #stubBalanceUpdateSuccess()}), and records file-import history
     * only when both a file name and hash were actually supplied. A PF-845 case: a description
     * with no existing merchant-description link saves with a null {@code merchantId} rather than
     * creating a new merchant or throwing -- that's the ordinary outcome for anything nobody has
     * linked yet.
     */
    @Nested
    @DisplayName("saveTransactions")
    class SaveTransactionsTests {
        @Test
        @DisplayName("should save unique transactions and update account balance")
        void shouldSaveAndBalance() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(BigDecimal.ZERO).version(1L).build();
            TransactionDto dto = TransactionDto.builder().description(DESCRIPTION_TEST).amount(BigDecimal.TEN).date(OffsetDateTime.now()).type(TransactionType.INCOME).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            stubBalanceUpdateSuccess();
            when(fileImportHistoryRepository.findByAccountIdAndFileHash(anyLong(), anyString())).thenReturn(Optional.empty());
            when(transactionRepository.findExistingForDuplicateCheck(anyLong(), any(), any())).thenReturn(List.of());
            when(merchantService.findMatchingMerchantIds(eq(USER_ID), any())).thenReturn(Map.of(DESCRIPTION_TEST, 1001L));

            // act
            int result = importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), IMPORT_FILENAME, "hash", false).saved();

            // assert & verify
            assertEquals(1, result);
            verify(transactionRepository).insertAll(anyList());
            verify(accountBalanceUpdateService).applyWithRetry(eq(USER_ID), eq(account), any());
            verify(fileImportHistoryRepository).save(any(FileImportHistory.class));
        }

        @Test
        @DisplayName("PF-845: should save with a null merchantId, not create one or throw, when the "
                + "description has no existing link -- that's the normal, expected outcome for "
                + "anything nobody has linked yet, not an edge case to fall back on")
        void shouldSaveWithNullMerchantIdWhenNoLinkExists() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(BigDecimal.ZERO).version(1L).build();
            TransactionDto dto = TransactionDto.builder().description("Unlinked Store").amount(BigDecimal.TEN).date(OffsetDateTime.now()).type(TransactionType.INCOME).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            stubBalanceUpdateSuccess();
            when(fileImportHistoryRepository.findByAccountIdAndFileHash(anyLong(), anyString())).thenReturn(Optional.empty());
            when(transactionRepository.findExistingForDuplicateCheck(anyLong(), any(), any())).thenReturn(List.of());
            when(merchantService.findMatchingMerchantIds(eq(USER_ID), any())).thenReturn(Map.of());

            // act
            int result = importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), IMPORT_FILENAME, "hash", false).saved();

            // assert & verify
            assertEquals(1, result);
            verify(transactionRepository).insertAll(argThat(list -> list.get(0).getMerchantId() == null));
            verify(merchantService, never()).recordDescriptionLink(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("should skip duplicates in database and batch")
        void shouldDetectBatchDuplicates() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(BigDecimal.ZERO).build();
            OffsetDateTime now = OffsetDateTime.now();
            TransactionDto dto1 = TransactionDto.builder().description("D1").amount(BigDecimal.TEN).date(now).type(TransactionType.INCOME).build();
            TransactionDto dto2 = TransactionDto.builder().description("D1").amount(BigDecimal.TEN).date(now).type(TransactionType.INCOME).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            stubBalanceUpdateSuccess();
            when(transactionRepository.findExistingForDuplicateCheck(anyLong(), any(), any())).thenReturn(List.of());
            when(merchantService.findMatchingMerchantIds(eq(USER_ID), any())).thenReturn(Map.of("D1", 1001L));

            // act
            TransactionImportService.SaveOutcome outcome = importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto1, dto2), null, null, false);
            int result = outcome.saved();

            // assert & verify
            assertEquals(1, result);
            assertEquals(1, outcome.duplicates());
            verify(transactionRepository).insertAll(argThat(list -> list.size() == 1));
        }

        @Test
        @DisplayName("should throw DuplicateImportException if file hash exists")
        void shouldThrowOnDuplicateHash() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(fileImportHistoryRepository.findByAccountIdAndFileHash(anyLong(), eq("existing"))).thenReturn(Optional.of(FileImportHistory.builder().build()));

            // act & assert & verify
            assertThrows(DuplicateImportException.class, () -> importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(), IMPORT_FILENAME, "existing", false));
        }

        @Test
        @DisplayName("should handle null fileName or fileHash separately")
        void shouldHandlePartialFileMetadata() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            TransactionDto dto = TransactionDto.builder().description("T").amount(BigDecimal.TEN).date(OffsetDateTime.now()).type(TransactionType.INCOME).build();

            // fileName is null case
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            stubBalanceUpdateSuccess();
            when(transactionRepository.findExistingForDuplicateCheck(anyLong(), any(), any())).thenReturn(List.of());
            when(merchantService.findMatchingMerchantIds(eq(USER_ID), any())).thenReturn(Map.of("T", 1001L));

            importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), null, "hash", false);
            verify(fileImportHistoryRepository, never()).save(any());

            // fileHash is null case
            reset(fileImportHistoryRepository);
            importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), IMPORT_FILENAME, null, false);
            verify(fileImportHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle empty approved list")
        void shouldHandleEmptyList() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // act
            int result = importService.saveTransactions(USER_ID, ACCOUNT_ID, Collections.emptyList(), null, null, false).saved();

            // assert & verify
            assertEquals(0, result);
            verify(transactionRepository, never()).insertAll(any());
        }

        @Test
        @DisplayName("should skip if transaction exists in database")
        void shouldSkipOnDatabaseMatch() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(BigDecimal.ZERO).build();
            TransactionDto dto = TransactionDto.builder().description("D1").amount(BigDecimal.TEN).date(OffsetDateTime.now()).type(TransactionType.INCOME).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            Transaction mockExisting = Transaction.builder()
                    .transactionDate(dto.date()).amount(dto.amount()).description(dto.description()).type(TransactionType.INCOME)
                    .build();
            when(transactionRepository.findExistingForDuplicateCheck(anyLong(), any(), any())).thenReturn(List.of(mockExisting));
            when(merchantService.findMatchingMerchantIds(eq(USER_ID), any())).thenReturn(Map.of("D1", 1001L));

            // act
            int result = importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), null, null, false).saved();

            // assert & verify
            assertEquals(0, result);
            verify(transactionRepository, never()).insertAll(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if account missing")
        void shouldThrowOnMissingAccount() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(), null, null, false));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own account during save")
        void shouldThrowOnAccessDenied() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(99L).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(), null, null, false));
        }

        @Test
        @DisplayName("should map category ID from TransactionDto to TransactionCreateRequest")
        void shouldMapCategoryId() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(BigDecimal.ZERO).version(1L).build();
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto categoryDto = com.mayureshpatel.pfdataservice.dto.category.CategoryDto.builder().id(42L).name("Test Category").build();
            TransactionDto dto = TransactionDto.builder()
                    .description(DESCRIPTION_TEST)
                    .amount(BigDecimal.TEN)
                    .date(OffsetDateTime.now())
                    .type(TransactionType.INCOME)
                    .category(categoryDto)
                    .build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(categoryRepository.findById(42L)).thenReturn(Optional.of(Category.builder().id(42L).userId(USER_ID).parentId(4L).build()));
            stubBalanceUpdateSuccess();
            when(transactionRepository.findExistingForDuplicateCheck(anyLong(), any(), any())).thenReturn(List.of());
            when(merchantService.findMatchingMerchantIds(eq(USER_ID), any())).thenReturn(Map.of(DESCRIPTION_TEST, 1001L));

            // act
            importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(dto), null, null, false);

            // assert & verify
            verify(transactionRepository).insertAll(argThat(list ->
                    list.size() == 1 && Long.valueOf(42L).equals(list.get(0).getCategoryId())
            ));
        }
    }

    /**
     * PF-898: a reviewed row's category must be one of the user's own subcategories -- the same
     * rule {@code TransactionService} applies to a manual edit -- and a violation is rejected
     * before anything is saved, as a {@link ResourceNotFoundException} so the bulk save reports
     * the file as FAILED with a safe message instead of a 403 for the whole batch.
     */
    @Nested
    @DisplayName("saveTransactions category validation")
    class SaveTransactionsCategoryTests {
        private TransactionDto rowWithCategory(Long categoryId) {
            return TransactionDto.builder()
                    .description(DESCRIPTION_TEST)
                    .amount(BigDecimal.TEN)
                    .date(OffsetDateTime.now())
                    .type(TransactionType.EXPENSE)
                    .category(com.mayureshpatel.pfdataservice.dto.category.CategoryDto.builder().id(categoryId).build())
                    .build();
        }

        private void stubAccount() {
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(BigDecimal.ZERO).version(1L).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        }

        @Test
        @DisplayName("saveTransactions_categoryOwnedByAnotherUser_isRejectedAndNothingSaved")
        void saveTransactions_categoryOwnedByAnotherUser_isRejectedAndNothingSaved() {
            // arrange
            stubAccount();
            when(categoryRepository.findById(50L)).thenReturn(Optional.of(Category.builder().id(50L).userId(999L).parentId(4L).build()));
            List<TransactionDto> rows = List.of(rowWithCategory(50L));

            // act & assert & verify
            ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                    () -> importService.saveTransactions(USER_ID, ACCOUNT_ID, rows, null, null, true));
            assertEquals("Category not found.", e.getMessage());
            verify(transactionRepository, never()).insertAll(anyList());
        }

        @Test
        @DisplayName("saveTransactions_missingCategory_isRejectedAndNothingSaved")
        void saveTransactions_missingCategory_isRejectedAndNothingSaved() {
            // arrange
            stubAccount();
            when(categoryRepository.findById(51L)).thenReturn(Optional.empty());
            List<TransactionDto> rows = List.of(rowWithCategory(51L));

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class,
                    () -> importService.saveTransactions(USER_ID, ACCOUNT_ID, rows, null, null, true));
            verify(transactionRepository, never()).insertAll(anyList());
        }

        @Test
        @DisplayName("saveTransactions_topLevelCategory_isRejectedAndNothingSaved")
        void saveTransactions_topLevelCategory_isRejectedAndNothingSaved() {
            // arrange
            stubAccount();
            when(categoryRepository.findById(4L)).thenReturn(Optional.of(Category.builder().id(4L).userId(USER_ID).build()));
            List<TransactionDto> rows = List.of(rowWithCategory(4L));

            // act & assert & verify
            ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                    () -> importService.saveTransactions(USER_ID, ACCOUNT_ID, rows, null, null, true));
            assertEquals("Only subcategories can be assigned to imported transactions.", e.getMessage());
            verify(transactionRepository, never()).insertAll(anyList());
        }

        @Test
        @DisplayName("saveTransactions_sameCategoryOnManyRows_isLookedUpOnce")
        void saveTransactions_sameCategoryOnManyRows_isLookedUpOnce() {
            // arrange
            stubAccount();
            stubBalanceUpdateSuccess();
            when(categoryRepository.findById(42L)).thenReturn(Optional.of(Category.builder().id(42L).userId(USER_ID).parentId(4L).build()));
            when(transactionRepository.findExistingForDuplicateCheck(anyLong(), any(), any())).thenReturn(List.of());
            TransactionDto second = TransactionDto.builder()
                    .description("other")
                    .amount(BigDecimal.ONE)
                    .date(OffsetDateTime.now())
                    .type(TransactionType.EXPENSE)
                    .category(com.mayureshpatel.pfdataservice.dto.category.CategoryDto.builder().id(42L).build())
                    .build();

            // act
            importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(rowWithCategory(42L), second), null, null, true);

            // assert & verify
            verify(categoryRepository, times(1)).findById(42L);
            verify(transactionRepository).insertAll(argThat(list -> list.size() == 2));
        }
    }

    /**
     * PF-898: a reviewed row's own merchant wins over the description-matched one, a row without
     * one falls back to the match, and a merchant the user doesn't own is rejected before anything
     * is saved.
     */
    @Nested
    @DisplayName("saveTransactions merchant precedence")
    class SaveTransactionsMerchantTests {
        private TransactionDto row(String description, Long merchantId) {
            return TransactionDto.builder()
                    .description(description)
                    .amount(BigDecimal.TEN)
                    .date(OffsetDateTime.now())
                    .type(TransactionType.EXPENSE)
                    .merchant(merchantId == null ? null : MerchantDto.builder().id(merchantId).build())
                    .build();
        }

        private Account stubHappyPath() {
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(BigDecimal.ZERO).version(1L).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            stubBalanceUpdateSuccess();
            when(transactionRepository.findExistingForDuplicateCheck(anyLong(), any(), any())).thenReturn(List.of());
            return account;
        }

        @Test
        @DisplayName("saveTransactions_rowWithMerchantOverride_savesThatMerchantId")
        void saveTransactions_rowWithMerchantOverride_savesThatMerchantId() {
            // arrange
            stubHappyPath();
            when(merchantService.findMatchingMerchantIds(eq(USER_ID), any())).thenReturn(Map.of(DESCRIPTION_TEST, 1001L));

            // act
            importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(row(DESCRIPTION_TEST, 77L)), null, null, false);

            // assert & verify
            verify(transactionRepository).insertAll(argThat(list -> Long.valueOf(77L).equals(list.get(0).getMerchantId())));
            verify(merchantService, never()).recordDescriptionLink(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("saveTransactions_rowWithoutMerchant_usesDescriptionMatchedMerchant")
        void saveTransactions_rowWithoutMerchant_usesDescriptionMatchedMerchant() {
            // arrange
            stubHappyPath();
            when(merchantService.findMatchingMerchantIds(eq(USER_ID), any())).thenReturn(Map.of(DESCRIPTION_TEST, 1001L));

            // act
            importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(row(DESCRIPTION_TEST, null)), null, null, false);

            // assert & verify
            verify(transactionRepository).insertAll(argThat(list -> Long.valueOf(1001L).equals(list.get(0).getMerchantId())));
        }

        @Test
        @DisplayName("saveTransactions_merchantsReviewedAndRowCleared_savesNoMerchant")
        void saveTransactions_merchantsReviewedAndRowCleared_savesNoMerchant() {
            // arrange
            stubHappyPath();

            // act
            importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(row(DESCRIPTION_TEST, null)), null, null, true);

            // assert & verify
            verify(transactionRepository).insertAll(argThat(list -> list.size() == 1 && list.get(0).getMerchantId() == null));
            verify(merchantService, never()).findMatchingMerchantIds(anyLong(), any());
        }

        @Test
        @DisplayName("saveTransactions_merchantsReviewedAndRowHasMerchant_savesThatMerchant")
        void saveTransactions_merchantsReviewedAndRowHasMerchant_savesThatMerchant() {
            // arrange
            stubHappyPath();

            // act
            importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(row(DESCRIPTION_TEST, 77L)), null, null, true);

            // assert & verify
            verify(transactionRepository).insertAll(argThat(list -> Long.valueOf(77L).equals(list.get(0).getMerchantId())));
        }

        @Test
        @DisplayName("saveTransactions_merchantNotOwnedByUser_isRejectedAndNothingSaved")
        void saveTransactions_merchantNotOwnedByUser_isRejectedAndNothingSaved() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(BigDecimal.ZERO).version(1L).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            doThrow(new ResourceNotFoundException("Merchant not found.")).when(merchantService).requireOwnedMerchants(eq(USER_ID), any());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class,
                    () -> importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(row(DESCRIPTION_TEST, 999L)), null, null, false));
            verify(transactionRepository, never()).insertAll(anyList());
        }
    }

    /**
     * {@code saveBulkTransactions} commits each request independently through {@code self} and
     * reports one result per request in request order: an already-imported file is reported as
     * skipped without affecting the others, any other runtime failure is reported as FAILED with
     * its message, and an ownership violation propagates instead of being swallowed.
     */
    @Nested
    @DisplayName("saveBulkTransactions")
    class SaveBulkTransactionsTests {
        private SaveTransactionRequest request(String fileName) {
            return new SaveTransactionRequest(List.of(), fileName, "hash-" + fileName, ACCOUNT_ID);
        }

        @Test
        @DisplayName("should still import the other files when one is already imported")
        void saveBulkTransactions_oneAlreadyImportedFile_stillImportsTheOthers() {
            // arrange
            SaveTransactionRequest first = request("a.csv");
            SaveTransactionRequest second = request("b.csv");
            when(self.saveTransactions(USER_ID, ACCOUNT_ID, first.transactions(), "a.csv", "hash-a.csv", false))
                    .thenThrow(new DuplicateImportException("This file has already been imported."));
            when(self.saveTransactions(USER_ID, ACCOUNT_ID, second.transactions(), "b.csv", "hash-b.csv", false))
                    .thenReturn(new TransactionImportService.SaveOutcome(3, 2));

            // act
            List<FileImportResultDto> results = importService.saveBulkTransactions(USER_ID, List.of(first, second));

            // assert & verify
            assertEquals(2, results.size());
            assertEquals(new FileImportResultDto("a.csv", FileImportStatus.SKIPPED_ALREADY_IMPORTED, 0, 0, null), results.get(0));
            assertEquals(new FileImportResultDto("b.csv", FileImportStatus.IMPORTED, 3, 2, null), results.get(1));
            verify(self).saveTransactions(USER_ID, ACCOUNT_ID, second.transactions(), "b.csv", "hash-b.csv", false);
        }

        @Test
        @DisplayName("should pass each request's merchantsReviewed flag through to the per-file save")
        void saveBulkTransactions_merchantsReviewedFlag_isPassedThrough() {
            // arrange
            SaveTransactionRequest reviewed = new SaveTransactionRequest(List.of(), "a.csv", "hash-a.csv", ACCOUNT_ID, true);
            when(self.saveTransactions(USER_ID, ACCOUNT_ID, List.of(), "a.csv", "hash-a.csv", true))
                    .thenReturn(new TransactionImportService.SaveOutcome(1, 0));

            // act
            List<FileImportResultDto> results = importService.saveBulkTransactions(USER_ID, List.of(reviewed));

            // assert & verify
            assertEquals(FileImportStatus.IMPORTED, results.get(0).status());
            verify(self).saveTransactions(USER_ID, ACCOUNT_ID, List.of(), "a.csv", "hash-a.csv", true);
        }

        @Test
        @DisplayName("should report an already-imported file as skipped")
        void saveBulkTransactions_alreadyImportedFile_reportsSkippedAlreadyImported() {
            // arrange
            when(self.saveTransactions(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenThrow(new DuplicateImportException("This file has already been imported."));

            // act
            List<FileImportResultDto> results = importService.saveBulkTransactions(USER_ID, List.of(request("a.csv")));

            // assert & verify
            assertEquals(1, results.size());
            assertEquals(FileImportStatus.SKIPPED_ALREADY_IMPORTED, results.get(0).status());
            assertEquals("a.csv", results.get(0).fileName());
            assertNull(results.get(0).error());
        }

        @Test
        @DisplayName("should report an unexpected failure with its message and continue")
        void saveBulkTransactions_unexpectedFailureOnOneFile_reportsFailedWithMessageAndContinues() {
            // arrange
            when(self.saveTransactions(USER_ID, ACCOUNT_ID, List.of(), "a.csv", "hash-a.csv", false))
                    .thenThrow(new ResourceNotFoundException("Account not found with ID: 10"));
            when(self.saveTransactions(USER_ID, ACCOUNT_ID, List.of(), "b.csv", "hash-b.csv", false))
                    .thenReturn(new TransactionImportService.SaveOutcome(1, 0));

            // act
            List<FileImportResultDto> results = importService.saveBulkTransactions(USER_ID, List.of(request("a.csv"), request("b.csv")));

            // assert & verify
            assertEquals(new FileImportResultDto("a.csv", FileImportStatus.FAILED, 0, 0, "Account not found with ID: 10"), results.get(0));
            assertEquals(new FileImportResultDto("b.csv", FileImportStatus.IMPORTED, 1, 0, null), results.get(1));
        }

        @Test
        @DisplayName("should hide raw exception text behind a generic message for data-access and null-message failures")
        void saveBulkTransactions_unsafeFailures_reportGenericMessage() {
            // arrange
            when(self.saveTransactions(USER_ID, ACCOUNT_ID, List.of(), "a.csv", "hash-a.csv", false))
                    .thenThrow(new org.springframework.dao.DataIntegrityViolationException("constraint transactions_pkey violated"));
            when(self.saveTransactions(USER_ID, ACCOUNT_ID, List.of(), "b.csv", "hash-b.csv", false))
                    .thenThrow(new IllegalStateException((String) null));

            // act
            List<FileImportResultDto> results = importService.saveBulkTransactions(USER_ID, List.of(request("a.csv"), request("b.csv")));

            // assert & verify
            assertEquals(new FileImportResultDto("a.csv", FileImportStatus.FAILED, 0, 0, "Could not import this file."), results.get(0));
            assertEquals(new FileImportResultDto("b.csv", FileImportStatus.FAILED, 0, 0, "Could not import this file."), results.get(1));
        }

        @Test
        @DisplayName("should propagate AccessDeniedException instead of reporting FAILED")
        void saveBulkTransactions_accessDenied_propagates() {
            // arrange
            when(self.saveTransactions(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenThrow(new AccessDeniedException("Access denied to account"));
            List<SaveTransactionRequest> requests = List.of(request("a.csv"));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> importService.saveBulkTransactions(USER_ID, requests));
        }
    }

    /**
     * {@code findAlreadyImported} returns the subset of (account, hash) candidates already in the
     * import history, enforcing ownership of every candidate account and loading each account once.
     */
    @Nested
    @DisplayName("findAlreadyImported")
    class FindAlreadyImportedTests {
        @Test
        @DisplayName("should return only the candidates whose hash is in the import history")
        void findAlreadyImported_returnsOnlyHashesInHistory() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(fileImportHistoryRepository.findByAccountIdAndFileHash(ACCOUNT_ID, "seen"))
                    .thenReturn(Optional.of(FileImportHistory.builder().build()));
            when(fileImportHistoryRepository.findByAccountIdAndFileHash(ACCOUNT_ID, "new"))
                    .thenReturn(Optional.empty());
            ImportHashCheckRequest seen = new ImportHashCheckRequest(ACCOUNT_ID, "seen");
            ImportHashCheckRequest fresh = new ImportHashCheckRequest(ACCOUNT_ID, "new");

            // act
            List<ImportHashCheckRequest> result = importService.findAlreadyImported(USER_ID, List.of(seen, fresh));

            // assert & verify
            assertEquals(List.of(seen), result);
            verify(accountRepository, times(1)).findById(ACCOUNT_ID);
        }

        @Test
        @DisplayName("should throw AccessDeniedException for another user's account")
        void findAlreadyImported_otherUsersAccount_throwsAccessDenied() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(999L).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            List<ImportHashCheckRequest> candidates = List.of(new ImportHashCheckRequest(ACCOUNT_ID, "h"));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> importService.findAlreadyImported(USER_ID, candidates));
            verifyNoInteractions(fileImportHistoryRepository);
        }

        @Test
        @DisplayName("should return an empty list for an empty candidate list")
        void findAlreadyImported_emptyList_returnsEmpty() {
            // act
            List<ImportHashCheckRequest> result = importService.findAlreadyImported(USER_ID, List.of());

            // assert & verify
            assertTrue(result.isEmpty());
            verifyNoInteractions(accountRepository, fileImportHistoryRepository);
        }
    }
}
