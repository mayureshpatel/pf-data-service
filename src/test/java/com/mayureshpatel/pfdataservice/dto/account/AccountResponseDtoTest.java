package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.dto.currency.CurrencyDto;
import com.mayureshpatel.pfdataservice.dto.user.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Account Response DTO Unit Tests")
class AccountResponseDtoTest {

    @Test
    @DisplayName("AccountDto: should correctly map all fields")
    void accountDtoShouldPopulateFields() {
        UserDto user = new UserDto(1L, "user", "user@example.com");
        AccountTypeDto type = AccountTypeDto.builder().code("SAVINGS").label("Savings").build();
        CurrencyDto currency = new CurrencyDto("USD", "US Dollar", "$", true);
        BankName bank = BankName.CAPITAL_ONE;
        BigDecimal balance = new BigDecimal("100.50");

        AccountDto dto = new AccountDto(1L, user, "My Savings", type, balance, currency, bank);

        assertEquals(1L, dto.id());
        assertEquals(user, dto.user());
        assertEquals("My Savings", dto.name());
        assertEquals(type, dto.type());
        assertEquals(balance, dto.currentBalance());
        assertEquals(currency, dto.currency());
        assertEquals(bank, dto.bank());
    }

    @Test
    @DisplayName("AccountTypeDto: should correctly map all fields via builder")
    void accountTypeDtoShouldPopulateFields() {
        AccountTypeDto dto = AccountTypeDto.builder()
                .code("SAVINGS")
                .label("Savings")
                .isAsset(true)
                .sortOrder(1)
                .isActive(true)
                .icon("piggy-bank")
                .color("#00FF00")
                .build();

        assertEquals("SAVINGS", dto.code());
        assertEquals("Savings", dto.label());
        assertTrue(dto.isAsset());
        assertEquals(1, dto.sortOrder());
        assertTrue(dto.isActive());
        assertEquals("piggy-bank", dto.icon());
        assertEquals("#00FF00", dto.color());

        AccountTypeDto updatedDto = dto.toBuilder().label("Updated Savings").build();
        assertEquals("SAVINGS", updatedDto.code());
        assertEquals("Updated Savings", updatedDto.label());
    }

    @Test
    @DisplayName("AccountSnapshotDto: should correctly map all fields")
    void accountSnapshotDtoShouldPopulateFields() {
        AccountDto account = new AccountDto(1L, null, "Savings", null, null, null, null);
        LocalDate date = LocalDate.now();
        BigDecimal balance = new BigDecimal("1000.00");

        AccountSnapshotDto dto = new AccountSnapshotDto(1L, account, date, balance);

        assertEquals(1L, dto.id());
        assertEquals(account, dto.account());
        assertEquals(date, dto.snapshotDate());
        assertEquals(balance, dto.balance());
    }
}
