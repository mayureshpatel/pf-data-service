package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Verifies {@code MerchantDefaultCategoryStrategy} delegates to {@code TransactionRepository}'s weighted lookup when a merchant/user id is present, short-circuits when either is missing, and reports the fixed lower-priority {@code getOrder()} this strategy relies on to run after {@code RuleBasedCategorizationStrategy}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantDefaultCategoryStrategy Unit Tests")
class MerchantDefaultCategoryStrategyTest {

    @Mock
    private TransactionRepository transactionRepository;

    private MerchantDefaultCategoryStrategy strategy;

    /** {@code categorize(Transaction, ...)} delegates to the repository's weighted lookup when both a merchant and a user id are present, and short-circuits to empty without querying when either is missing. */
    @Nested
    @DisplayName("categorize (Domain)")
    class CategorizeDomainTests {
        @Test
        @DisplayName("should return the repository's weighted default when merchant and user are present")
        void shouldReturnWeightedDefault() {
            // arrange
            strategy = new MerchantDefaultCategoryStrategy(transactionRepository);
            Transaction t = Transaction.builder().merchant(Merchant.builder().id(5L).build()).build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .userId(1L)
                    .build();
            when(transactionRepository.findWeightedDefaultCategoryId(eq(5L), eq(1L), anyLong(), anyInt()))
                    .thenReturn(Optional.of(42L));

            // act
            Optional<Long> result = strategy.categorize(t, context);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals(42L, result.get());
        }

        @Test
        @DisplayName("should return empty and never query when the transaction has no merchant")
        void shouldReturnEmptyWhenNoMerchant() {
            // arrange
            strategy = new MerchantDefaultCategoryStrategy(transactionRepository);
            Transaction t = Transaction.builder().merchant(null).build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .userId(1L)
                    .build();

            // act
            Optional<Long> result = strategy.categorize(t, context);

            // assert & verify
            assertTrue(result.isEmpty());
            verifyNoInteractions(transactionRepository);
        }

        @Test
        @DisplayName("should return empty and never query when the context has no user id")
        void shouldReturnEmptyWhenNoUserId() {
            // arrange
            strategy = new MerchantDefaultCategoryStrategy(transactionRepository);
            Transaction t = Transaction.builder().merchant(Merchant.builder().id(5L).build()).build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .userId(null)
                    .build();

            // act
            Optional<Long> result = strategy.categorize(t, context);

            // assert & verify
            assertTrue(result.isEmpty());
            verifyNoInteractions(transactionRepository);
        }
    }

    /** The {@link TransactionUpdateRequest} overload follows the same delegate-or-short-circuit behavior as {@link CategorizeDomainTests}, keyed on {@code merchantId} directly rather than a hydrated {@code Merchant}. */
    @Nested
    @DisplayName("categorize (Update Request)")
    class CategorizeRequestTests {
        @Test
        @DisplayName("should return the repository's weighted default when merchantId and user are present")
        void shouldReturnWeightedDefault() {
            // arrange
            strategy = new MerchantDefaultCategoryStrategy(transactionRepository);
            TransactionUpdateRequest request = TransactionUpdateRequest.builder().merchantId(5L).build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .userId(1L)
                    .build();
            when(transactionRepository.findWeightedDefaultCategoryId(eq(5L), eq(1L), anyLong(), anyInt()))
                    .thenReturn(Optional.of(42L));

            // act
            Optional<Long> result = strategy.categorize(request, context);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals(42L, result.get());
        }

        @Test
        @DisplayName("should return empty and never query when the request has no merchantId")
        void shouldReturnEmptyWhenNoMerchantId() {
            // arrange
            strategy = new MerchantDefaultCategoryStrategy(transactionRepository);
            TransactionUpdateRequest request = TransactionUpdateRequest.builder().merchantId(null).build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .userId(1L)
                    .build();

            // act
            Optional<Long> result = strategy.categorize(request, context);

            // assert & verify
            assertTrue(result.isEmpty());
            verifyNoInteractions(transactionRepository);
        }
    }

    @Test
    @DisplayName("getOrder should be lower priority than RuleBasedCategorizationStrategy's")
    void shouldRunAfterRuleBasedStrategy() {
        // arrange
        strategy = new MerchantDefaultCategoryStrategy(transactionRepository);
        RuleBasedCategorizationStrategy ruleBasedStrategy = new RuleBasedCategorizationStrategy();

        // act & assert -- lower getOrder() runs first (TransactionCategorizer sorts ascending)
        assertTrue(strategy.getOrder() > ruleBasedStrategy.getOrder());
    }
}
