package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
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

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryRuleRepository categoryRuleRepository;

    @InjectMocks
    private SecurityService securityService;

    private static final Long USER_ID = 1L;
    private static final Long ANOTHER_USER_ID = 2L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long TRANSACTION_ID = 100L;
    private static final Long CATEGORY_ID = 200L;
    private static final Long RULE_ID = 300L;

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

    private Category buildCategory(Long userId) {
        return Category.builder()
                .id(CATEGORY_ID)
                .userId(userId)
                .name("Dining Out")
                .build();
    }

    private CategoryRule buildRule(Long userId) {
        return CategoryRule.builder()
                .id(RULE_ID)
                .user(User.builder().id(userId).build())
                .keyword("Coffee")
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
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(transaction));

            // Act
            boolean result = securityService.isTransactionOwner(TRANSACTION_ID, userDetails);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when transaction is not found or not owned")
        void isTransactionOwner_notOwned_returnsFalse() {
            // Arrange
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.empty());

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

    @Nested
    @DisplayName("isCategoryOwner")
    class IsCategoryOwnerTest {
        @Test
        @DisplayName("should return true when user owns the category")
        void isCategoryOwner_matchingId_returnsTrue() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            Category category = buildCategory(USER_ID);
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            boolean result = securityService.isCategoryOwner(CATEGORY_ID, userDetails);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when mismatched userId")
        void isCategoryOwner_mismatchedId_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            Category category = buildCategory(ANOTHER_USER_ID);
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            boolean result = securityService.isCategoryOwner(CATEGORY_ID, userDetails);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isRuleOwner")
    class IsRuleOwnerTest {
        @Test
        @DisplayName("should return true when user owns the rule")
        void isRuleOwner_matchingId_returnsTrue() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            CategoryRule rule = buildRule(USER_ID);
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));

            boolean result = securityService.isRuleOwner(RULE_ID, userDetails);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when mismatched userId")
        void isRuleOwner_mismatchedId_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            CategoryRule rule = buildRule(ANOTHER_USER_ID);
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));

            boolean result = securityService.isRuleOwner(RULE_ID, userDetails);

            assertThat(result).isFalse();
        }
    }
}
