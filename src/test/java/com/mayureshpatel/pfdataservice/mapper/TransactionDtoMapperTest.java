package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransactionDtoMapper Unit Tests")
class TransactionDtoMapperTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // Arrange
        Constructor<TransactionDtoMapper> constructor = TransactionDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act
        TransactionDtoMapper instance = constructor.newInstance();

        // Assert
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("Method: toDto")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when source is null")
        void toDto_shouldReturnNullWhenSourceIsNull() {
            // Act
            TransactionDto result = TransactionDtoMapper.toDto(null);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // Arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .amount(new BigDecimal("100.00"))
                    .transactionDate(now)
                    .description("Test Transaction")
                    .type(TransactionType.EXPENSE)
                    .build();

            // Act
            TransactionDto dto = TransactionDtoMapper.toDto(transaction);

            // Assert
            assertNotNull(dto);
            assertEquals(transaction.getId(), dto.id());
            assertEquals(transaction.getAmount(), dto.amount());
            assertEquals(transaction.getTransactionDate(), dto.date());
            assertEquals(transaction.getDescription(), dto.description());
            assertEquals(transaction.getType(), dto.type());
        }
    }
}
