package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecurringTransactionDtoMapper Unit Tests")
class RecurringTransactionDtoMapperTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // Arrange
        Constructor<RecurringTransactionDtoMapper> constructor = RecurringTransactionDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act
        RecurringTransactionDtoMapper instance = constructor.newInstance();

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
            RecurringTransactionDto result = RecurringTransactionDtoMapper.toDto(null);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // Arrange
            LocalDate nextDate = LocalDate.now().plusDays(30);
            RecurringTransaction rt = RecurringTransaction.builder()
                    .id(1L)
                    .userId(100L)
                    .amount(new BigDecimal("15.99"))
                    .frequency("MONTHLY")
                    .nextDate(nextDate)
                    .active(true)
                    .build();

            // Act
            RecurringTransactionDto dto = RecurringTransactionDtoMapper.toDto(rt);

            // Assert
            assertNotNull(dto);
            assertEquals(rt.getId(), dto.id());
            assertEquals(rt.getUserId(), dto.userId());
            assertEquals(rt.getAmount(), dto.amount());
            assertEquals("MONTHLY", dto.frequency().name());
            assertEquals(rt.getNextDate(), dto.nextDate());
            assertTrue(dto.active());
        }

        @Test
        @DisplayName("should handle null userId")
        void toDto_shouldHandleNullUserId() {
            // Arrange
            RecurringTransaction rt = RecurringTransaction.builder()
                    .id(1L)
                    .userId(null)
                    .frequency("WEEKLY")
                    .build();

            // Act
            RecurringTransactionDto dto = RecurringTransactionDtoMapper.toDto(rt);

            // Assert
            assertNotNull(dto);
            assertNull(dto.userId());
        }
    }
}
