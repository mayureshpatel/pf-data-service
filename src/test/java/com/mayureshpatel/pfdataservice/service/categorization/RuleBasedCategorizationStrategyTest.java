package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RuleBasedCategorizationStrategy Unit Tests")
class RuleBasedCategorizationStrategyTest {

    private final RuleBasedCategorizationStrategy strategy = new RuleBasedCategorizationStrategy();

    @Nested
    @DisplayName("categorize (Domain)")
    class CategorizeDomainTests {
        @Test
        @DisplayName("should return category ID if keyword matches description case-insensitively")
        void shouldMatchKeyword() {
            // Arrange
            Transaction t = Transaction.builder().description("AMAZON MARKETPLACE").build();
            CategoryRule rule = CategoryRule.builder()
                    .keyword("Amazon")
                    .category(Category.builder().id(10L).build())
                    .build();

            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(rule))
                    .build();

            // Act
            Optional<Long> result = strategy.categorize(t, context);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(10L, result.get());
        }

        @Test
        @DisplayName("should return empty if no rules match")
        void shouldNotMatch() {
            // Arrange
            Transaction t = Transaction.builder().description("Unknown").build();
            CategoryRule rule = CategoryRule.builder().keyword("Amazon").build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(rule))
                    .build();

            // Act
            Optional<Long> result = strategy.categorize(t, context);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty if description or rules are missing")
        void shouldHandleNulls() {
            // Act & Assert
            assertTrue(strategy.categorize(Transaction.builder().description(null).build(), CategorizationStrategy.CategorizationContext.builder().rules(List.of()).build()).isEmpty());
            assertTrue(strategy.categorize(Transaction.builder().description("Test").build(), CategorizationStrategy.CategorizationContext.builder().rules(null).build()).isEmpty());
        }

        @Test
        @DisplayName("should handle empty rules list")
        void shouldHandleEmptyRules() {
            // Act
            Optional<Long> result = strategy.categorize(Transaction.builder().description("Test").build(),
                    CategorizationStrategy.CategorizationContext.builder().rules(List.of()).build());

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("categorize (Request)")
    class CategorizeRequestTests {
        @Test
        @DisplayName("should match keyword for update request")
        void shouldMatchRequest() {
            // Arrange
            TransactionUpdateRequest req = TransactionUpdateRequest.builder().description("Netflix.com").build();
            CategoryRule rule = CategoryRule.builder()
                    .keyword("Netflix")
                    .category(Category.builder().id(20L).build())
                    .build();

            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(rule))
                    .build();

            // Act
            Optional<Long> result = strategy.categorize(req, context);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(20L, result.get());
        }

        @Test
        @DisplayName("should return empty if description or rules are missing for request")
        void shouldHandleNullsInRequest() {
            // Act & Assert
            assertTrue(strategy.categorize(TransactionUpdateRequest.builder().description(null).build(), CategorizationStrategy.CategorizationContext.builder().rules(List.of()).build()).isEmpty());
            assertTrue(strategy.categorize(TransactionUpdateRequest.builder().description("Test").build(), CategorizationStrategy.CategorizationContext.builder().rules(null).build()).isEmpty());
        }
    }

    @Test
    void shouldReturnOrder() {
        assertEquals(100, strategy.getOrder());
    }
}
