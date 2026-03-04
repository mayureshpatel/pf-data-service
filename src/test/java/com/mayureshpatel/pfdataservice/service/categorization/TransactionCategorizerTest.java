package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionCategorizer unit tests")
class TransactionCategorizerTest {

    @Mock
    private CategorizationStrategy strategyOrder10;

    @Mock
    private CategorizationStrategy strategyOrder20;

    private static final Long CATEGORY_ID = 42L;

    private Transaction buildTransaction() {
        return new Transaction();
    }

    @Nested
    @DisplayName("guessCategory(tx, rules, categories) — three-arg overload")
    class GuessCategoryThreeArgTests {

        @Test
        @DisplayName("should return -1 when no strategies are registered")
        void guessCategory_noStrategies_returnsMinusOne() {
            TransactionCategorizer categorizer = new TransactionCategorizer(List.of());

            Long result = categorizer.guessCategory(buildTransaction(), List.of(), null);

            assertThat(result).isEqualTo(-1L);
        }

        @Test
        @DisplayName("should return the category ID when the single strategy matches")
        void guessCategory_singleStrategyMatches_returnsCategoryId() {
            when(strategyOrder10.categorize(any(Transaction.class), any())).thenReturn(Optional.of(CATEGORY_ID));
            TransactionCategorizer categorizer = new TransactionCategorizer(List.of(strategyOrder10));

            Long result = categorizer.guessCategory(buildTransaction(), List.of(), null);

            assertThat(result).isEqualTo(CATEGORY_ID);
        }

        @Test
        @DisplayName("should return -1 when the single strategy does not match")
        void guessCategory_singleStrategyNoMatch_returnsMinusOne() {
            when(strategyOrder10.categorize(any(Transaction.class), any())).thenReturn(Optional.empty());
            TransactionCategorizer categorizer = new TransactionCategorizer(List.of(strategyOrder10));

            Long result = categorizer.guessCategory(buildTransaction(), List.of(), null);

            assertThat(result).isEqualTo(-1L);
        }

        @Test
        @DisplayName("should execute strategies in ascending order of getOrder()")
        void guessCategory_multipleStrategies_executedInAscendingOrder() {
            when(strategyOrder10.getOrder()).thenReturn(10);
            when(strategyOrder20.getOrder()).thenReturn(20);
            when(strategyOrder10.categorize(any(Transaction.class), any())).thenReturn(Optional.empty());
            when(strategyOrder20.categorize(any(Transaction.class), any())).thenReturn(Optional.of(CATEGORY_ID));

            // Registered in reverse order to prove sorting by getOrder() occurs
            TransactionCategorizer categorizer = new TransactionCategorizer(List.of(strategyOrder20, strategyOrder10));

            Long result = categorizer.guessCategory(buildTransaction(), List.of(), null);

            assertThat(result).isEqualTo(CATEGORY_ID);
            InOrder order = inOrder(strategyOrder10, strategyOrder20);
            order.verify(strategyOrder10).categorize(any(Transaction.class), any());
            order.verify(strategyOrder20).categorize(any(Transaction.class), any());
        }

        @Test
        @DisplayName("should stop at the first strategy that returns a match")
        void guessCategory_firstStrategyMatches_secondStrategyNotCalled() {
            when(strategyOrder10.getOrder()).thenReturn(10);
            when(strategyOrder20.getOrder()).thenReturn(20);
            when(strategyOrder10.categorize(any(Transaction.class), any())).thenReturn(Optional.of(CATEGORY_ID));
            TransactionCategorizer categorizer = new TransactionCategorizer(List.of(strategyOrder10, strategyOrder20));

            Long result = categorizer.guessCategory(buildTransaction(), List.of(), null);

            assertThat(result).isEqualTo(CATEGORY_ID);
            verify(strategyOrder20, never()).categorize(any(Transaction.class), any());
        }

        @Test
        @DisplayName("should return -1 when all strategies return empty")
        void guessCategory_allStrategiesReturnEmpty_returnsMinusOne() {
            when(strategyOrder10.getOrder()).thenReturn(10);
            when(strategyOrder20.getOrder()).thenReturn(20);
            when(strategyOrder10.categorize(any(Transaction.class), any())).thenReturn(Optional.empty());
            when(strategyOrder20.categorize(any(Transaction.class), any())).thenReturn(Optional.empty());
            TransactionCategorizer categorizer = new TransactionCategorizer(List.of(strategyOrder10, strategyOrder20));

            Long result = categorizer.guessCategory(buildTransaction(), List.of(), null);

            assertThat(result).isEqualTo(-1L);
        }
    }

    @Nested
    @DisplayName("guessCategory(tx, rules) — two-arg overload")
    class GuessCategoryTwoArgTests {

        @Test
        @DisplayName("should delegate to the three-arg overload and return the matched category ID")
        void guessCategory_twoArgOverload_delegatesToThreeArgOverload() {
            when(strategyOrder10.categorize(any(Transaction.class), any())).thenReturn(Optional.of(99L));
            TransactionCategorizer categorizer = new TransactionCategorizer(List.of(strategyOrder10));

            Long result = categorizer.guessCategory(buildTransaction(), List.of());

            assertThat(result).isEqualTo(99L);
        }

        @Test
        @DisplayName("should return -1 when no strategy matches via two-arg overload")
        void guessCategory_twoArgNoMatch_returnsMinusOne() {
            when(strategyOrder10.categorize(any(Transaction.class), any())).thenReturn(Optional.empty());
            TransactionCategorizer categorizer = new TransactionCategorizer(List.of(strategyOrder10));

            Long result = categorizer.guessCategory(buildTransaction(), List.of());

            assertThat(result).isEqualTo(-1L);
        }
    }
}
