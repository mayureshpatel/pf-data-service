package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.transaction.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.util.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecurringTransactionDtoMapper unit tests")
class RecurringTransactionDtoMapperTest {

    @Test
    @DisplayName("should return null when recurring transaction is null")
    void toDto_null_returnsNull() {
        assertThat(RecurringTransactionDtoMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("should map all fields correctly")
    void toDto_fullRecurring_mapsAllFields() {
        RecurringTransaction rt = TestFixtures.aRecurringTransaction();

        RecurringTransactionDto dto = RecurringTransactionDtoMapper.toDto(rt);

        assertThat(dto.id()).isEqualTo(rt.getId());
        assertThat(dto.userId()).isEqualTo(rt.getUser().getId());
        assertThat(dto.amount()).isEqualByComparingTo(rt.getAmount());
        assertThat(dto.frequency()).isEqualTo(rt.getFrequency());
        assertThat(dto.lastDate()).isEqualTo(rt.getLastDate());
        assertThat(dto.nextDate()).isEqualTo(rt.getNextDate());
        assertThat(dto.active()).isEqualTo(rt.isActive());

        assertThat(dto.account()).isNotNull();
        assertThat(dto.account().id()).isEqualTo(rt.getAccount().getId());
        assertThat(dto.merchant()).isNotNull();
        assertThat(dto.merchant().id()).isEqualTo(rt.getMerchant().getId());
    }

    @Test
    @DisplayName("should handle null user, account, and merchant")
    void toDto_nullOptionalFields_mapsNulls() {
        RecurringTransaction rt = new RecurringTransaction();
        rt.setId(1L);
        rt.setActive(false);

        RecurringTransactionDto dto = RecurringTransactionDtoMapper.toDto(rt);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.userId()).isNull();
        assertThat(dto.account()).isNull();
        assertThat(dto.merchant()).isNull();
        assertThat(dto.active()).isFalse();
    }
}
