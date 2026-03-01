package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.util.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccountDtoMapper unit tests")
class AccountDtoMapperTest {

    @Test
    @DisplayName("should return null when account is null")
    void toDto_nullAccount_returnsNull() {
        assertThat(AccountDtoMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("should map all fields correctly")
    void toDto_fullAccount_mapsAllFields() {
        Account account = TestFixtures.anAccount(TestFixtures.aUser());
        account.setBankName(BankName.DISCOVER);

        AccountDto dto = AccountDtoMapper.toDto(account);

        assertThat(dto.id()).isEqualTo(account.getId());
        assertThat(dto.userId()).isEqualTo(account.getUser().getId());
        assertThat(dto.name()).isEqualTo(account.getName());
        assertThat(dto.accountTypeCode()).isEqualTo(account.getType().getCode());
        assertThat(dto.accountTypeLabel()).isEqualTo(account.getType().getLabel());
        assertThat(dto.currentBalance()).isEqualByComparingTo(account.getCurrentBalance());
        assertThat(dto.currencyCode()).isEqualTo(account.getCurrency().getCode());
        assertThat(dto.currencySymbol()).isEqualTo(account.getCurrency().getSymbol());
        assertThat(dto.bankName()).isEqualTo("Discover");
    }

    @Test
    @DisplayName("should handle null optional fields (user, type, currency, bankName)")
    void toDto_nullOptionalFields_mapsNulls() {
        Account account = new Account();
        account.setId(1L);
        account.setName("Bare Account");

        AccountDto dto = AccountDtoMapper.toDto(account);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Bare Account");
        assertThat(dto.userId()).isNull();
        assertThat(dto.accountTypeCode()).isNull();
        assertThat(dto.accountTypeLabel()).isNull();
        assertThat(dto.currencyCode()).isNull();
        assertThat(dto.currencySymbol()).isNull();
        assertThat(dto.bankName()).isNull();
    }
}
