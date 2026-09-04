package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
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

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private SecurityService securityService;

    private static final Long USER_ID = 1L;
    private static final Long ANOTHER_USER_ID = 2L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long TRANSACTION_ID = 100L;
    private static final Long CATEGORY_ID = 200L;
    private static final Long RULE_ID = 300L;
    private static final Long BUDGET_ID = 400L;
    private static final Long RECURRING_ID = 500L;
    private static final Long MERCHANT_ID = 600L;

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

    private Budget buildBudget(Long userId) {
        return Budget.builder()
                .id(BUDGET_ID)
                .userId(userId)
                .build();
    }

    private RecurringTransaction buildRecurringTransaction(Long userId) {
        return RecurringTransaction.builder()
                .id(RECURRING_ID)
                .userId(userId)
                .build();
    }

    private Merchant buildMerchant(Long userId) {
        return Merchant.builder()
                .id(MERCHANT_ID)
                .userId(userId)
                .originalName("STARBUCKS #1")
                .cleanName("Starbucks")
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

    @Nested
    @DisplayName("isBudgetOwner")
    class IsBudgetOwnerTest {
        @Test
        @DisplayName("should return true when user owns the budget")
        void isBudgetOwner_matchingId_returnsTrue() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            Budget budget = buildBudget(USER_ID);
            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(budget));

            boolean result = securityService.isBudgetOwner(BUDGET_ID, userDetails);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when mismatched userId")
        void isBudgetOwner_mismatchedId_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            Budget budget = buildBudget(ANOTHER_USER_ID);
            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(budget));

            boolean result = securityService.isBudgetOwner(BUDGET_ID, userDetails);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when budget not found")
        void isBudgetOwner_notFound_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.empty());

            boolean result = securityService.isBudgetOwner(BUDGET_ID, userDetails);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when args are null")
        void isBudgetOwner_nullArgs_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            assertThat(securityService.isBudgetOwner(null, userDetails)).isFalse();
            assertThat(securityService.isBudgetOwner(BUDGET_ID, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("isRecurringTransactionOwner")
    class IsRecurringTransactionOwnerTest {
        @Test
        @DisplayName("should return true when user owns the recurring transaction")
        void isRecurringTransactionOwner_matchingId_returnsTrue() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            RecurringTransaction recurringTransaction = buildRecurringTransaction(USER_ID);
            when(recurringTransactionRepository.findById(RECURRING_ID)).thenReturn(Optional.of(recurringTransaction));

            boolean result = securityService.isRecurringTransactionOwner(RECURRING_ID, userDetails);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when mismatched userId")
        void isRecurringTransactionOwner_mismatchedId_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            RecurringTransaction recurringTransaction = buildRecurringTransaction(ANOTHER_USER_ID);
            when(recurringTransactionRepository.findById(RECURRING_ID)).thenReturn(Optional.of(recurringTransaction));

            boolean result = securityService.isRecurringTransactionOwner(RECURRING_ID, userDetails);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when recurring transaction not found")
        void isRecurringTransactionOwner_notFound_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            when(recurringTransactionRepository.findById(RECURRING_ID)).thenReturn(Optional.empty());

            boolean result = securityService.isRecurringTransactionOwner(RECURRING_ID, userDetails);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when args are null")
        void isRecurringTransactionOwner_nullArgs_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            assertThat(securityService.isRecurringTransactionOwner(null, userDetails)).isFalse();
            assertThat(securityService.isRecurringTransactionOwner(RECURRING_ID, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("isMerchantOwner")
    class IsMerchantOwnerTest {

        @Test
        @DisplayName("should return true when user owns the merchant")
        void isMerchantOwner_matchingId_returnsTrue() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            Merchant merchant = buildMerchant(USER_ID);
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            boolean result = securityService.isMerchantOwner(MERCHANT_ID, userDetails);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when mismatched userId")
        void isMerchantOwner_mismatchedId_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            Merchant merchant = buildMerchant(ANOTHER_USER_ID);
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            boolean result = securityService.isMerchantOwner(MERCHANT_ID, userDetails);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("PF-220: should return false, not throw, for a global merchant (userId is null)")
        void isMerchantOwner_globalMerchant_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            Merchant globalMerchant = buildMerchant(null);
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(globalMerchant));

            boolean result = securityService.isMerchantOwner(MERCHANT_ID, userDetails);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when merchant not found")
        void isMerchantOwner_notFound_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.empty());

            boolean result = securityService.isMerchantOwner(MERCHANT_ID, userDetails);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when args are null")
        void isMerchantOwner_nullArgs_returnsFalse() {
            CustomUserDetails userDetails = buildUserDetails(USER_ID);
            assertThat(securityService.isMerchantOwner(null, userDetails)).isFalse();
            assertThat(securityService.isMerchantOwner(MERCHANT_ID, null)).isFalse();
        }
    }
}
