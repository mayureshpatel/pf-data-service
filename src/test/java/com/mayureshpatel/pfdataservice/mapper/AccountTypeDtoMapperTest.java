package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;
import com.mayureshpatel.pfdataservice.util.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccountTypeDtoMapper unit tests")
class AccountTypeDtoMapperTest {

    @Test
    @DisplayName("should return null when accountType is null")
    void toDto_null_returnsNull() {
        assertThat(AccountTypeDtoMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("should map all fields correctly with iconography")
    void toDto_fullAccountType_mapsAllFields() {
        AccountType type = TestFixtures.anAccountType();
        type.setIconography(new Iconography("bank", "#336699"));

        AccountTypeDto dto = AccountTypeDtoMapper.toDto(type);

        assertThat(dto.code()).isEqualTo(type.getCode());
        assertThat(dto.label()).isEqualTo(type.getLabel());
        assertThat(dto.isAsset()).isEqualTo(type.isAsset());
        assertThat(dto.sortOrder()).isEqualTo(type.getSortOrder());
        assertThat(dto.isActive()).isEqualTo(type.isActive());
        assertThat(dto.icon()).isEqualTo("bank");
        assertThat(dto.color()).isEqualTo("#336699");
    }

    @Test
    @DisplayName("should handle null iconography")
    void toDto_nullIconography_mapsIconAndColorAsNull() {
        AccountType type = TestFixtures.anAccountType(); // iconography is null by default

        AccountTypeDto dto = AccountTypeDtoMapper.toDto(type);

        assertThat(dto.code()).isEqualTo("SAVINGS");
        assertThat(dto.icon()).isNull();
        assertThat(dto.color()).isNull();
    }
}
