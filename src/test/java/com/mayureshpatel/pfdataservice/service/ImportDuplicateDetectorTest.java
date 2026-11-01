package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.exception.DuplicateImportException;
import com.mayureshpatel.pfdataservice.repository.file_import_history.FileImportHistoryRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.ImportDuplicateDetector.DedupeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link ImportDuplicateDetector}'s signature definition, existing-signature lookup, and
 * batch de-duplication in isolation (PF-898: extracted from {@code TransactionImportService}, whose
 * own tests still cover the same behavior end to end through preview and save).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImportDuplicateDetector Unit Tests")
class ImportDuplicateDetectorTest {

    private static final Long ACCOUNT_ID = 10L;
    private static final String COFFEE = "COFFEE";
    private static final String AMOUNT_FIVE = "5.00";
    private static final String HASH = "hash-a.csv";
    private static final String FILE_NAME = "a.csv";
    private static final OffsetDateTime EARLY = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime LATE = OffsetDateTime.of(2026, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private FileImportHistoryRepository fileImportHistoryRepository;

    private ImportDuplicateDetector detector;
    private Account account;

    /** Builds the detector against the mocked repositories and a fixed target account. */
    @BeforeEach
    void setUp() {
        detector = new ImportDuplicateDetector(transactionRepository, fileImportHistoryRepository);
        account = Account.builder().id(ACCOUNT_ID).userId(1L).build();
    }

    /** Builds an approved row with the given description and amount on the early date. */
    private static TransactionDto dto(String description, String amount, MerchantDto merchant) {
        return TransactionDto.builder().date(EARLY).description(description).amount(new BigDecimal(amount))
                .type(TransactionType.EXPENSE).merchant(merchant).build();
    }

    @Test
    @DisplayName("buildSignature_trailingZerosDiffer_producesSameSignature")
    void buildSignature_trailingZerosDiffer_producesSameSignature() {
        // arrange
        BigDecimal shortScale = new BigDecimal("5.0");
        BigDecimal longScale = new BigDecimal(AMOUNT_FIVE);

        // act
        String first = ImportDuplicateDetector.buildSignature(EARLY, shortScale, COFFEE, TransactionType.EXPENSE);
        String second = ImportDuplicateDetector.buildSignature(EARLY, longScale, COFFEE, TransactionType.EXPENSE);

        // assert & verify
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("buildSignature_nullType_isAccepted")
    void buildSignature_nullType_isAccepted() {
        // act
        String signature = ImportDuplicateDetector.buildSignature(EARLY, BigDecimal.ONE, COFFEE, null);

        // assert & verify
        assertThat(signature).endsWith("_COFFEE_null");
    }

    @Test
    @DisplayName("findExistingSignatures_queriesDateRangeOfIncomingRows")
    void findExistingSignatures_queriesDateRangeOfIncomingRows() {
        // arrange
        Transaction existing = Transaction.builder().transactionDate(EARLY).amount(new BigDecimal(AMOUNT_FIVE))
                .description(COFFEE).type(TransactionType.EXPENSE).build();
        when(transactionRepository.findExistingForDuplicateCheck(ACCOUNT_ID, EARLY, LATE)).thenReturn(List.of(existing));

        // act
        Set<String> signatures = detector.findExistingSignatures(ACCOUNT_ID, List.of(LATE, EARLY));

        // assert & verify
        assertThat(signatures).containsExactly(
                ImportDuplicateDetector.buildSignature(EARLY, new BigDecimal("5"), COFFEE, TransactionType.EXPENSE));
        verify(transactionRepository).findExistingForDuplicateCheck(ACCOUNT_ID, EARLY, LATE);
    }

    @Test
    @DisplayName("findExistingSignatures_noDates_fallsBackToNow")
    void findExistingSignatures_noDates_fallsBackToNow() {
        // arrange
        when(transactionRepository.findExistingForDuplicateCheck(any(), any(), any())).thenReturn(List.of());

        // act
        Set<String> signatures = detector.findExistingSignatures(ACCOUNT_ID, List.of());

        // assert & verify
        assertThat(signatures).isEmpty();
        verify(transactionRepository).findExistingForDuplicateCheck(any(), any(), any());
    }

    @Test
    @DisplayName("deduplicate_skipsExistingAndRepeatedRows")
    void deduplicate_skipsExistingAndRepeatedRows() {
        // arrange
        TransactionDto existingRow = dto("RENT", "900.00", null);
        TransactionDto firstCoffee = dto(COFFEE, AMOUNT_FIVE, null);
        TransactionDto repeatCoffee = dto(COFFEE, "5.0", null);
        Set<String> existing = Set.of(ImportDuplicateDetector.buildSignature(EARLY, new BigDecimal("900"), "RENT", TransactionType.EXPENSE));

        // act
        DedupeResult result = detector.deduplicate(List.of(existingRow, firstCoffee, repeatCoffee), existing, account, Map.of());

        // assert & verify
        assertThat(result.duplicateCount()).isEqualTo(2);
        assertThat(result.uniqueTransactions()).hasSize(1);
        TransactionCreateRequest saved = result.uniqueTransactions().get(0);
        assertThat(saved.getDescription()).isEqualTo(COFFEE);
        assertThat(saved.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(saved.getType()).isEqualTo("EXPENSE");
    }

    @Test
    @DisplayName("deduplicate_rowMerchantWinsOverDescriptionMatch")
    void deduplicate_rowMerchantWinsOverDescriptionMatch() {
        // arrange
        TransactionDto withMerchant = dto(COFFEE, AMOUNT_FIVE, MerchantDto.builder().id(7L).build());
        TransactionDto withoutMerchant = dto("LUNCH", "9.00", null);
        Map<String, Long> matches = Map.of(COFFEE, 99L, "LUNCH", 42L);

        // act
        DedupeResult result = detector.deduplicate(List.of(withMerchant, withoutMerchant), Set.of(), account, matches);

        // assert & verify
        assertThat(result.duplicateCount()).isZero();
        assertThat(result.uniqueTransactions()).extracting(TransactionCreateRequest::getMerchantId).containsExactly(7L, 42L);
    }

    @Test
    @DisplayName("deduplicate_mapsCategoryIdAndLeavesUnmatchedMerchantNull")
    void deduplicate_mapsCategoryIdAndLeavesUnmatchedMerchantNull() {
        // arrange
        TransactionDto row = TransactionDto.builder().date(EARLY).postDate(LATE).description(COFFEE)
                .amount(BigDecimal.ONE).type(TransactionType.EXPENSE).category(CategoryDto.builder().id(3L).build()).build();

        // act
        DedupeResult result = detector.deduplicate(List.of(row), Set.of(), account, Map.of());

        // assert & verify
        TransactionCreateRequest saved = result.uniqueTransactions().get(0);
        assertThat(saved.getCategoryId()).isEqualTo(3L);
        assertThat(saved.getPostDate()).isEqualTo(LATE);
        assertThat(saved.getMerchantId()).isNull();
    }

    @Test
    @DisplayName("requireNotAlreadyImported_knownHash_throwsDuplicateImport")
    void requireNotAlreadyImported_knownHash_throwsDuplicateImport() {
        // arrange
        when(fileImportHistoryRepository.findByAccountIdAndFileHash(ACCOUNT_ID, HASH))
                .thenReturn(Optional.of(FileImportHistory.builder().build()));

        // act & assert & verify
        assertThatThrownBy(() -> detector.requireNotAlreadyImported(ACCOUNT_ID, HASH))
                .isInstanceOf(DuplicateImportException.class)
                .hasMessage("This file has already been imported.");
    }

    @Test
    @DisplayName("requireNotAlreadyImported_nullHash_skipsHistoryLookup")
    void requireNotAlreadyImported_nullHash_skipsHistoryLookup() {
        // act
        detector.requireNotAlreadyImported(ACCOUNT_ID, null);

        // assert & verify
        verifyNoInteractions(fileImportHistoryRepository);
    }

    @Test
    @DisplayName("isAlreadyImported_unknownHash_returnsFalse")
    void isAlreadyImported_unknownHash_returnsFalse() {
        // arrange
        when(fileImportHistoryRepository.findByAccountIdAndFileHash(ACCOUNT_ID, HASH)).thenReturn(Optional.empty());

        // act
        boolean imported = detector.isAlreadyImported(ACCOUNT_ID, HASH);

        // assert & verify
        assertThat(imported).isFalse();
    }

    @Test
    @DisplayName("recordImport_withNameAndHash_savesHistory")
    void recordImport_withNameAndHash_savesHistory() {
        // act
        detector.recordImport(account, FILE_NAME, HASH, 3);

        // assert & verify
        ArgumentCaptor<FileImportHistory> captor = ArgumentCaptor.forClass(FileImportHistory.class);
        verify(fileImportHistoryRepository).save(captor.capture());
        FileImportHistory history = captor.getValue();
        assertThat(history.getAccount()).isSameAs(account);
        assertThat(history.getFileName()).isEqualTo(FILE_NAME);
        assertThat(history.getFileHash()).isEqualTo(HASH);
        assertThat(history.getTransactionCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("recordImport_missingHash_savesNothing")
    void recordImport_missingHash_savesNothing() {
        // act
        detector.recordImport(account, FILE_NAME, null, 3);

        // assert & verify
        verify(fileImportHistoryRepository, never()).save(any());
    }
}
