package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.FileImportHistoryRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParser;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionImportServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private FileImportHistoryRepository fileImportHistoryRepository;
    @Mock private TransactionParserFactory parserFactory;
    @Mock private TransactionCategorizer categorizer;
    @Mock private TransactionParser parser;

    private TransactionImportService importService;

    @BeforeEach
    void setUp() {
        importService = new TransactionImportService(
                transactionRepository,
                accountRepository,
                fileImportHistoryRepository,
                parserFactory,
                categorizer
        );
    }

    @Test
    void previewTransactions_ShouldThrowIfFileAlreadyImported() {
        byte[] content = "test data".getBytes();
        String hash = importService.calculateFileHash(content);

        when(fileImportHistoryRepository.existsByAccountIdAndFileHash(1L, hash)).thenReturn(true);

        assertThatThrownBy(() -> importService.previewTransactions(1L, "BANK", content, "test.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already been imported");
    }

    @Test
    void previewTransactions_ShouldParseAndCategorize() {
        byte[] content = "test data".getBytes();
        String hash = importService.calculateFileHash(content);

        when(fileImportHistoryRepository.existsByAccountIdAndFileHash(1L, hash)).thenReturn(false);
        when(parserFactory.getTransactionParser("BANK")).thenReturn(parser);

        List<Transaction> parsedTransactions = new ArrayList<>();
        Transaction t1 = new Transaction();
        t1.setDate(LocalDate.now());
        t1.setDescription("Grocery Store");
        t1.setAmount(BigDecimal.TEN);
        t1.setType(TransactionType.EXPENSE);
        parsedTransactions.add(t1);

        when(parser.parse(eq(1L), any(InputStream.class))).thenReturn(parsedTransactions);
        when(categorizer.guessCategory(t1)).thenReturn("Groceries");

        List<TransactionPreview> result = importService.previewTransactions(1L, "BANK", content, "test.csv");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDescription()).isEqualTo("Grocery Store");
        assertThat(result.getFirst().getSuggestedCategory()).isEqualTo("Groceries");
    }

    @Test
    void saveTransactions_ShouldSkipDuplicates() {
        Long accountId = 1L;
        Account account = new Account();
        account.setId(accountId);
        account.setCurrentBalance(BigDecimal.ZERO);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // Transaction 1: Exists (Duplicate)
        Transaction t1 = new Transaction();
        t1.setDate(LocalDate.now());
        t1.setDescription("Duplicate Txn");
        t1.setAmount(BigDecimal.TEN);
        t1.setType(TransactionType.EXPENSE);

        // Transaction 2: New (Unique)
        Transaction t2 = new Transaction();
        t2.setDate(LocalDate.now());
        t2.setDescription("New Txn");
        t2.setAmount(BigDecimal.valueOf(20));
        t2.setType(TransactionType.EXPENSE);

        List<Transaction> inputList = List.of(t1, t2);

        when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
                eq(accountId), eq(t1.getDate()), eq(t1.getAmount()), eq(t1.getDescription()), eq(t1.getType())
        )).thenReturn(true); // T1 Exists

        when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
                eq(accountId), eq(t2.getDate()), eq(t2.getAmount()), eq(t2.getDescription()), eq(t2.getType())
        )).thenReturn(false); // T2 Does Not Exist

        // Execute
        int savedCount = importService.saveTransactions(accountId, inputList, "file.csv", "hash123");

        // Assert
        assertThat(savedCount).isEqualTo(1); // Only T2 should be saved
        verify(transactionRepository, times(1)).saveAll(anyList());
        verify(fileImportHistoryRepository, times(1)).save(any());

        // Verify balance update logic (only for unique txn: -20)
        // Note: Logic in service is: currentBalance + (-20)
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("-20");
        verify(accountRepository).save(account);
    }
}