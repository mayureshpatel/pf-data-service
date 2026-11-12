package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.BalanceMode;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.transfer.TransferMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link TransactionService} create, edit and delete leave a MANUAL-balance account's
 * stored balance unchanged. The real {@link AccountBalanceUpdateService} runs over a mocked
 * repository so the balance actually written by {@code updateBalance} can be inspected.
 */
@DisplayName("TransactionService with a MANUAL balance account")
class ManualBalanceTransactionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long TX_ID = 100L;
    private static final BigDecimal BALANCE = new BigDecimal("1000.00");

    private TransactionRepository transactionRepository;
    private AccountRepository accountRepository;
    private TransactionService service;
    private Account manual;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        accountRepository = mock(AccountRepository.class);
        service = new TransactionService(transactionRepository, accountRepository, mock(CategoryRepository.class),
                mock(TransactionCategorizer.class), mock(CategoryRuleRepository.class), mock(TransferMatcher.class),
                mock(MerchantService.class), new AccountBalanceUpdateService(accountRepository));
        manual = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(BALANCE).version(1L)
                .balanceMode(BalanceMode.MANUAL).build();
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(manual));
    }

    private void assertBalanceWrittenUnchanged() {
        ArgumentCaptor<BigDecimal> written = ArgumentCaptor.forClass(BigDecimal.class);
        verify(accountRepository).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), written.capture(), anyLong());
        assertEquals(0, BALANCE.compareTo(written.getValue()));
    }

    @Test
    @DisplayName("createTransaction leaves the balance unchanged")
    void create_shouldNotChangeManualBalance() {
        // arrange
        when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);
        TransactionCreateRequest request = TransactionCreateRequest.builder().accountId(ACCOUNT_ID)
                .amount(new BigDecimal("250.00")).type("CONTRIBUTION").description("401k").build();

        // act
        service.createTransaction(USER_ID, request);

        // assert & verify
        assertBalanceWrittenUnchanged();
    }

    @Test
    @DisplayName("updateTransaction leaves the balance unchanged")
    void update_shouldNotChangeManualBalance() {
        // arrange
        Transaction original = Transaction.builder().id(TX_ID).account(manual).amount(new BigDecimal("250.00"))
                .type(TransactionType.CONTRIBUTION).build();
        when(transactionRepository.findById(TX_ID, USER_ID)).thenReturn(Optional.of(original));
        when(transactionRepository.update(eq(USER_ID), any(Transaction.class))).thenReturn(1);
        TransactionUpdateRequest request = TransactionUpdateRequest.builder().id(TX_ID).accountId(ACCOUNT_ID)
                .amount(new BigDecimal("400.00")).type("EARNINGS").description("edited").build();

        // act
        service.updateTransaction(USER_ID, request);

        // assert & verify
        assertBalanceWrittenUnchanged();
    }

    @Test
    @DisplayName("deleteTransaction leaves the balance unchanged")
    void delete_shouldNotChangeManualBalance() {
        // arrange
        Transaction existing = Transaction.builder().id(TX_ID).account(manual).amount(new BigDecimal("250.00"))
                .type(TransactionType.CONTRIBUTION).build();
        when(transactionRepository.findById(TX_ID, USER_ID)).thenReturn(Optional.of(existing));

        // act
        service.deleteTransaction(USER_ID, TX_ID);

        // assert & verify
        assertBalanceWrittenUnchanged();
    }
}
