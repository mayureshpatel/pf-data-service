package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityService unit tests")
class SecurityServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SecurityService securityService;

    private static final Long USER_ID = 1L;
    private static final Long ANOTHER_USER_ID = 2L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long TRANSACTION_ID = 100L;

    private CustomUserDetails buildUserDetails(Long userId) {
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .passwordHash("hash")
                .email("test@example.com")
                .build();
        return new CustomUserDetails(user);
    }

    private Account buildAccount(Long userId) {
        return Account.builder()
                .id(ACCOUNT_ID)
                .userId(userId)
                .name("Savings")
                .build();
    }

    private Transaction buildTransaction(Long userId) {
        return Transaction.builder()
                .id(TRANSACTION_ID)
                .account(buildAccount(userId))
                .build();
    }

    @Nested
    @DisplayName("isAccountOwner")
    class IsAccountOwnerTest {

        @Test
        @DisplayName("should return true when user owns the account")
        void isAccountOwner_matchingId_returnsTrue() {
            // Arrange
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            Account account = buildAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // Act
            boolean result = securityService.isAccountOwner(ACCOUNT_ID, userDetails);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when user does not own the account")
        void isAccountOwner_mismatchedId_returnsFalse() {
            // Arrange
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            Account account = buildAccount(ANOTHER_USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // Act
            boolean result = securityService.isAccountOwner(ACCOUNT_ID, userDetails);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when account is not found")
        void isAccountOwner_recordNotFound_returnsFalse() {
            // Arrange
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // Act
            boolean result = securityService.isAccountOwner(ACCOUNT_ID, userDetails);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when accountId is null")
        void isAccountOwner_nullAccountId_returnsFalse() {
            // Arrange
            CustomUserDetails userDetails = buildUserDetails(USER_ID);

            // Act
            boolean result = securityService.isAccountOwner(null, userDetails);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when userDetails is null")
        void isAccountOwner_nullUserDetails_returnsFalse() {
            // Act
            boolean result = securityService.isAccountOwner(ACCOUNT_ID, null);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isTransactionOwner")
    class IsTransactionOwnerTest {

        @Test
        @DisplayName("should return true when user owns the transaction")
        void isTransactionOwner_matchingId_returnsTrue() {
            // Arrange
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            Transaction transaction = buildTransaction(USER_ID);
            when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));

            // Act
            boolean result = securityService.isTransactionOwner(TRANSACTION_ID, userDetails);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when user does not own the transaction")
        void isTransactionOwner_mismatchedId_returnsFalse() {
            // Arrange
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            Transaction transaction = buildTransaction(ANOTHER_USER_ID);
            when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));

            // Act
            boolean result = securityService.isTransactionOwner(TRANSACTION_ID, userDetails);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when transaction is not found")
        void isTransactionOwner_recordNotFound_returnsFalse() {
            // Arrange
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

            // Act
            boolean result = securityService.isTransactionOwner(TRANSACTION_ID, userDetails);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when transactionId is null")
        void isTransactionOwner_nullTransactionId_returnsFalse() {
            // Arrange
            CustomUserDetails userDetails = buildUserDetails(USER_ID);

            // Act
            boolean result = securityService.isTransactionOwner(null, userDetails);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when userDetails is null")
        void isTransactionOwner_nullUserDetails_returnsFalse() {
            // Act
            boolean result = securityService.isTransactionOwner(TRANSACTION_ID, null);

            // Assert
            assertThat(result).isFalse();
        }
    }
}
