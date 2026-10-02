package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.dto.currency.CurrencyDto;
import com.mayureshpatel.pfdataservice.dto.user.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the accessors/builders of the three read-side account DTOs -- {@link AccountDto}, {@link AccountTypeDto}, and {@link AccountSnapshotDto} -- map their fields 1:1, grouped here since each is a small, closely-related response shape rather than its own top-level concern. */
@DisplayName("Account Response DTO Unit Tests")
class AccountResponseDtoTest {

    private static final String TYPE_CODE = "SAVINGS";
    private static final String TYPE_LABEL = "Savings";

    @Test
    @DisplayName("AccountDto: should correctly map all fields")
    void accountDtoShouldPopulateFields() {
        UserDto user = new UserDto(1L, "user", "user@example.com");
        AccountTypeDto type = AccountTypeDto.builder().code(TYPE_CODE).label(TYPE_LABEL).build();
        CurrencyDto currency = new CurrencyDto("USD", "US Dollar", "$", true);
        BankName bank = BankName.CAPITAL_ONE;
        BigDecimal balance = new BigDecimal("100.50");

        AccountDto dto = new AccountDto(1L, user, "My Savings", type, balance, currency, bank, 1L);

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
                .code(TYPE_CODE)
                .label(TYPE_LABEL)
                .isAsset(true)
                .sortOrder(1)
                .isActive(true)
                .icon("piggy-bank")
                .color("#00FF00")
                .build();

        assertEquals(TYPE_CODE, dto.code());
        assertEquals(TYPE_LABEL, dto.label());
        assertTrue(dto.isAsset());
        assertEquals(1, dto.sortOrder());
        assertTrue(dto.isActive());
        assertEquals("piggy-bank", dto.icon());
        assertEquals("#00FF00", dto.color());

        AccountTypeDto updatedDto = dto.toBuilder().label("Updated Savings").build();
        assertEquals(TYPE_CODE, updatedDto.code());
        assertEquals("Updated Savings", updatedDto.label());
    }

    @Test
    @DisplayName("AccountSnapshotDto: should correctly map all fields")
    void accountSnapshotDtoShouldPopulateFields() {
        AccountDto account = new AccountDto(1L, null, TYPE_LABEL, null, null, null, null, 1L);
        LocalDate date = LocalDate.now();
        BigDecimal balance = new BigDecimal("1000.00");

        AccountSnapshotDto dto = new AccountSnapshotDto(1L, account, date, balance);

        assertEquals(1L, dto.id());
        assertEquals(account, dto.account());
        assertEquals(date, dto.snapshotDate());
        assertEquals(balance, dto.balance());
    }
}
