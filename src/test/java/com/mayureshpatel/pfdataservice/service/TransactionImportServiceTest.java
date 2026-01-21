package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.model.User;
import com.mayureshpatel.pfdataservice.repository.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.FileImportHistoryRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.categorization.VendorCleaner;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParser;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionImportServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private FileImportHistoryRepository fileImportHistoryRepository;
    @Mock private TransactionParserFactory parserFactory;
    @Mock private TransactionCategorizer categorizer;
    @Mock private VendorCleaner vendorCleaner;
    @Mock private TransactionParser parser;

    private TransactionImportService importService;
    private Account account;
    private User user;

    @BeforeEach
    void setUp() {
        importService = new TransactionImportService(
                transactionRepository,
                accountRepository,
                fileImportHistoryRepository,
                parserFactory,
                categorizer,
                vendorCleaner
        );
        
        user = new User();
        user.setId(10L);
        
        account = new Account();
        account.setId(1L);
        account.setUser(user);
        account.setCurrentBalance(BigDecimal.ZERO);
    }

    @Test
    void previewTransactions_ShouldParseAndCategorize() {
        InputStream is = new ByteArrayInputStream("test data".getBytes());

        when(parserFactory.getTransactionParser("BANK")).thenReturn(parser);

        Transaction t1 = new Transaction();
        t1.setDate(LocalDate.now());
        t1.setDescription("Grocery Store");
        t1.setAmount(BigDecimal.TEN);
        t1.setType(TransactionType.EXPENSE);

        when(parser.parse(eq(1L), any(InputStream.class))).thenReturn(Stream.of(t1));
        when(categorizer.loadRulesForUser(10L)).thenReturn(List.of());
        when(categorizer.guessCategory(eq(t1), anyList())).thenReturn("Groceries");
        when(vendorCleaner.loadRulesForUser(10L)).thenReturn(List.of());
        when(vendorCleaner.cleanVendorName(any(), anyList())).thenReturn("Vendor");

        List<TransactionPreview> result = importService.previewTransactions(10L, 1L, "BANK", is, "test.csv");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().description()).isEqualTo("Grocery Store");
        assertThat(result.getFirst().suggestedCategory()).isEqualTo("Groceries");
        assertThat(result.getFirst().vendorName()).isEqualTo("Vendor");
    }

    @Test
    void saveTransactions_ShouldSkipDuplicates() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // Transaction 1: Exists (Duplicate)
        TransactionDto t1 = TransactionDto.builder()
                .date(LocalDate.now())
                .description("Duplicate Txn")
                .amount(BigDecimal.TEN)
                .type(TransactionType.EXPENSE)
                .build();

        // Transaction 2: New (Unique)
        TransactionDto t2 = TransactionDto.builder()
                .date(LocalDate.now())
                .description("New Txn")
                .amount(BigDecimal.valueOf(20))
                .type(TransactionType.EXPENSE)
                .build();

        List<TransactionDto> inputList = List.of(t1, t2);

        when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
                eq(1L), eq(t1.date()), eq(t1.amount()), eq(t1.description()), eq(t1.type())
        )).thenReturn(true); // T1 Exists

        when(transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
                eq(1L), eq(t2.date()), eq(t2.amount()), eq(t2.description()), eq(t2.type())
        )).thenReturn(false); // T2 Does Not Exist

        // Execute
        int savedCount = importService.saveTransactions(10L, 1L, inputList, "file.csv", "hash123");

        // Assert
        assertThat(savedCount).isEqualTo(1); // Only T2 should be saved
        verify(transactionRepository, times(1)).saveAll(anyList());
        verify(fileImportHistoryRepository, times(1)).save(any());

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("-20");
        verify(accountRepository).save(account);
    }
}
