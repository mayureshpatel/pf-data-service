package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.mapper.RecurringTransactionDtoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecurringTransaction domain object tests")
class RecurringTransactionTest {

    @Test
    @DisplayName("toDto — should map all fields correctly")
    void toDto_mapsAllFields() {
        // Arrange
        User user = new User();
        user.setId(1L);

        Account account = new Account();
        account.setId(5L);
        account.setName("Test Account");

        Merchant merchant = new Merchant();
        merchant.setId(8L);
        merchant.setCleanName("Netflix");

        RecurringTransaction rt = new RecurringTransaction();
        rt.setId(10L);
        rt.setUser(user);
        rt.setAccount(account);
        rt.setMerchant(merchant);
        rt.setAmount(new BigDecimal("15.99"));
        rt.setFrequency(Frequency.MONTHLY);
        rt.setLastDate(LocalDate.of(2026, 1, 1));
        rt.setNextDate(LocalDate.of(2026, 2, 1));
        rt.setActive(true);

        // Act
        RecurringTransactionDto dto = RecurringTransactionDtoMapper.toDto(rt);

        // Assert
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.userId()).isEqualTo(1L);
        assertThat(dto.account().id()).isEqualTo(5L);
        assertThat(dto.merchant().id()).isEqualTo(8L);
        assertThat(dto.amount()).isEqualByComparingTo("15.99");
        assertThat(dto.frequency()).isEqualTo(Frequency.MONTHLY);
        assertThat(dto.lastDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(dto.nextDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(dto.active()).isTrue();
    }
}
