package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TransactionCategorizer Unit Tests")
class TransactionCategorizerTest {

    private List<CategorizationStrategy> strategies;
    private TransactionCategorizer categorizer;

    @Mock
    private CategorizationStrategy s1;
    @Mock
    private CategorizationStrategy s2;

    @BeforeEach
    void setUp() {
        strategies = new ArrayList<>();
        categorizer = new TransactionCategorizer(strategies);
    }

    @Nested
    @DisplayName("guessCategory (Domain Transaction)")
    class GuessCategoryDomainTests {
        @Test
        @DisplayName("should return -1L if no strategies match")
        void shouldReturnDefaultIfNoMatches() {
            // Arrange
            when(s1.getOrder()).thenReturn(1);
            when(s1.categorize(any(Transaction.class), any())).thenReturn(Optional.empty());
            strategies.add(s1);

            // Act
            Long result = categorizer.guessCategory(Transaction.builder().build(), List.of());

            // Assert
            assertEquals(-1L, result);
        }

        @Test
        @DisplayName("should call strategies in order and return first match")
        void shouldFollowOrder() {
            // Arrange
            when(s1.getOrder()).thenReturn(200);
            when(s2.getOrder()).thenReturn(100);

            when(s2.categorize(any(Transaction.class), any())).thenReturn(Optional.of(10L));

            strategies.add(s1);
            strategies.add(s2);

            // Act
            Long result = categorizer.guessCategory(Transaction.builder().build(), List.of());

            // Assert
            assertEquals(10L, result);
            verify(s2).categorize(any(Transaction.class), any());
            verify(s1, never()).categorize(any(Transaction.class), any());
        }

        @Test
        @DisplayName("should handle null account or user in context")
        void shouldHandleNullContextInfo() {
            // Arrange
            when(s1.getOrder()).thenReturn(1);
            when(s1.categorize(any(Transaction.class), any())).thenReturn(Optional.empty());
            strategies.add(s1);

            // Act
            // Case 1: account is null
            categorizer.guessCategory(Transaction.builder().account(null).build(), List.of());
            // Case 2: userId is null
            categorizer.guessCategory(Transaction.builder().account(Account.builder().userId(null).build()).build(), List.of());

            // Assert
            verify(s1, times(2)).categorize(any(Transaction.class), any());
        }
    }

    @Nested
    @DisplayName("guessCategory (Update Request)")
    class GuessCategoryRequestTests {
        @Test
        @DisplayName("should return match for update request")
        void shouldHandleRequest() {
            // Arrange
            when(s1.getOrder()).thenReturn(1);
            when(s1.categorize(any(TransactionUpdateRequest.class), any())).thenReturn(Optional.of(50L));
            strategies.add(s1);

            TransactionUpdateRequest request = TransactionUpdateRequest.builder().description("Test").build();

            // Act
            Long result = categorizer.guessCategory(1L, request, List.of(), List.of());

            // Assert
            assertEquals(50L, result);
            verify(s1).categorize(any(TransactionUpdateRequest.class), any());
        }

        @Test
        @DisplayName("should return -1L for request if no match")
        void shouldReturnDefaultIfNoMatchForRequest() {
            // Arrange
            when(s1.getOrder()).thenReturn(1);
            when(s1.categorize(any(TransactionUpdateRequest.class), any())).thenReturn(Optional.empty());
            strategies.add(s1);

            // Act
            Long result = categorizer.guessCategory(1L, TransactionUpdateRequest.builder().build(), List.of(), List.of());

            // Assert
            assertEquals(-1L, result);
        }
    }
}
