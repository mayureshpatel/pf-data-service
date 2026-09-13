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
        // arrange
        Constructor<TransactionDtoMapper> constructor = TransactionDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // act
        TransactionDtoMapper instance = constructor.newInstance();

        // assert & verify
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("Method: toDto")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when source is null")
        void toDto_shouldReturnNullWhenSourceIsNull() {
            // act
            TransactionDto result = TransactionDtoMapper.toDto(null);

            // assert & verify
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .amount(new BigDecimal("100.00"))
                    .transactionDate(now)
                    .description("Test Transaction")
                    .type(TransactionType.EXPENSE)
                    .build();

            // act
            TransactionDto dto = TransactionDtoMapper.toDto(transaction);

            // assert & verify
            assertNotNull(dto);
            assertEquals(transaction.getId(), dto.id());
            assertEquals(transaction.getAmount(), dto.amount());
            assertEquals(transaction.getTransactionDate(), dto.date());
            assertEquals(transaction.getDescription(), dto.description());
            assertEquals(transaction.getType(), dto.type());
        }
    }
}
