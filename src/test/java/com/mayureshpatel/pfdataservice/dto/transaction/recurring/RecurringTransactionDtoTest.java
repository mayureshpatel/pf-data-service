package com.mayureshpatel.pfdataservice.dto.transaction.recurring;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RecurringTransactionDto Structure Tests")
class RecurringTransactionDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        AccountDto account = AccountDto.builder().id(1L).build();
        MerchantDto merchant = MerchantDto.builder().id(2L).build();
        BigDecimal amount = new BigDecimal("100.00");
        Frequency frequency = Frequency.MONTHLY;
        LocalDate lastDate = LocalDate.now().minusMonths(1);
        LocalDate nextDate = LocalDate.now().plusMonths(1);

        RecurringTransactionDto dto = RecurringTransactionDto.builder()
                .id(1L)
                .userId(10L)
                .account(account)
                .merchant(merchant)
                .amount(amount)
                .frequency(frequency)
                .lastDate(lastDate)
                .nextDate(nextDate)
                .active(true)
                .build();

        assertEquals(1L, dto.id());
        assertEquals(10L, dto.userId());
        assertEquals(account, dto.account());
        assertEquals(merchant, dto.merchant());
        assertEquals(amount, dto.amount());
        assertEquals(frequency, dto.frequency());
        assertEquals(lastDate, dto.lastDate());
        assertEquals(nextDate, dto.nextDate());
        assertTrue(dto.active());
    }
}
