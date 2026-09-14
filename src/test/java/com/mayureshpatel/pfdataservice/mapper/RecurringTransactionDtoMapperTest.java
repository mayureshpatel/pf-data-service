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
        // arrange
        Constructor<RecurringTransactionDtoMapper> constructor = RecurringTransactionDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // act
        RecurringTransactionDtoMapper instance = constructor.newInstance();

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
            RecurringTransactionDto result = RecurringTransactionDtoMapper.toDto(null);

            // assert & verify
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // arrange
            LocalDate nextDate = LocalDate.now().plusDays(30);
            RecurringTransaction rt = RecurringTransaction.builder()
                    .id(1L)
                    .userId(100L)
                    .amount(new BigDecimal("15.99"))
                    .frequency("MONTHLY")
                    .nextDate(nextDate)
                    .active(true)
                    .build();

            // act
            RecurringTransactionDto dto = RecurringTransactionDtoMapper.toDto(rt);

            // assert & verify
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
            // arrange
            RecurringTransaction rt = RecurringTransaction.builder()
                    .id(1L)
                    .userId(null)
                    .frequency("WEEKLY")
                    .build();

            // act
            RecurringTransactionDto dto = RecurringTransactionDtoMapper.toDto(rt);

            // assert & verify
            assertNotNull(dto);
            assertNull(dto.userId());
        }
    }
}
