package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.account.AccountSnapshotRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnapshotService Unit Tests")
class SnapshotServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountSnapshotRepository snapshotRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SnapshotService snapshotService;

    private static final Long ACCOUNT_ID = 10L;
    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("createEndOfMonthSnapshot")
    class CreateEndOfMonthSnapshotTests {

        @Test
        @DisplayName("should create new snapshot when none exists and user owns account")
        void shouldCreateNewSnapshot() {
            // Arrange
            LocalDate dateInMonth = LocalDate.of(2026, 3, 15);
            LocalDate endOfMonth = LocalDate.of(2026, 3, 31);
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(new BigDecimal("1000.00")).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.getNetFlowAfterDate(eq(ACCOUNT_ID), eq(endOfMonth))).thenReturn(new BigDecimal("100.00"));
            when(snapshotRepository.findByAccountIdAndSnapshotDate(ACCOUNT_ID, endOfMonth)).thenReturn(Optional.empty());

            // Act
            snapshotService.createEndOfMonthSnapshot(USER_ID, ACCOUNT_ID, dateInMonth);

            // Assert
            verify(snapshotRepository).insert(argThat(s ->
                    s.getBalance().compareTo(new BigDecimal("900.00")) == 0 &&
                            s.getSnapshotDate().equals(endOfMonth) &&
                            s.getAccount().getId().equals(ACCOUNT_ID)
            ));
        }

        @Test
        @DisplayName("should update existing snapshot when it already exists and user owns account")
        void shouldUpdateExistingSnapshot() {
            // Arrange
            LocalDate dateInMonth = LocalDate.of(2026, 3, 15);
            LocalDate endOfMonth = LocalDate.of(2026, 3, 31);
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(new BigDecimal("1000.00")).build();
            AccountSnapshot existing = AccountSnapshot.builder().id(1L).balance(BigDecimal.ZERO).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.getNetFlowAfterDate(eq(ACCOUNT_ID), eq(endOfMonth))).thenReturn(BigDecimal.ZERO);
            when(snapshotRepository.findByAccountIdAndSnapshotDate(ACCOUNT_ID, endOfMonth)).thenReturn(Optional.of(existing));

            // Act
            snapshotService.createEndOfMonthSnapshot(USER_ID, ACCOUNT_ID, dateInMonth);

            // Assert
            verify(snapshotRepository).update(argThat(s ->
                    s.getId().equals(1L) &&
                            s.getBalance().compareTo(new BigDecimal("1000.00")) == 0
            ));
        }

        @Test
        @DisplayName("should handle null net flow as zero")
        void shouldHandleNullNetFlow() {
            // Arrange
            LocalDate dateInMonth = LocalDate.of(2026, 3, 15);
            LocalDate endOfMonth = LocalDate.of(2026, 3, 31);
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(new BigDecimal("1000.00")).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.getNetFlowAfterDate(anyLong(), any())).thenReturn(null);
            when(snapshotRepository.findByAccountIdAndSnapshotDate(anyLong(), any())).thenReturn(Optional.empty());

            // Act
            snapshotService.createEndOfMonthSnapshot(USER_ID, ACCOUNT_ID, dateInMonth);

            // Assert
            verify(snapshotRepository).insert(argThat(s -> s.getBalance().compareTo(new BigDecimal("1000.00")) == 0));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if account not found")
        void shouldThrowOnAccountNotFound() {
            // Arrange
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> snapshotService.createEndOfMonthSnapshot(USER_ID, ACCOUNT_ID, LocalDate.now()));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own account")
        void shouldThrowOnAccessDenied() {
            // Arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(99L).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> snapshotService.createEndOfMonthSnapshot(USER_ID, ACCOUNT_ID, LocalDate.now()));
        }
    }
}
