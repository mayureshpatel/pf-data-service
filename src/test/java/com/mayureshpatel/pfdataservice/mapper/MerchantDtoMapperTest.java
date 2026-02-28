package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.util.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MerchantDtoMapper unit tests")
class MerchantDtoMapperTest {

    @Test
    @DisplayName("should return null when merchant is null")
    void toDto_nullMerchant_returnsNull() {
        assertThat(MerchantDtoMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("should map all fields correctly")
    void toDto_fullMerchant_mapsAllFields() {
        Merchant merchant = TestFixtures.aMerchant();

        MerchantDto dto = MerchantDtoMapper.toDto(merchant);

        assertThat(dto.id()).isEqualTo(merchant.getId());
        assertThat(dto.userId()).isEqualTo(merchant.getUser().getId());
        assertThat(dto.originalName()).isEqualTo(merchant.getOriginalName());
        assertThat(dto.cleanName()).isEqualTo(merchant.getCleanName());
    }

    @Test
    @DisplayName("should handle null user")
    void toDto_nullUser_mapsUserIdAsNull() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setOriginalName("TEST");
        merchant.setCleanName("Test");

        MerchantDto dto = MerchantDtoMapper.toDto(merchant);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.userId()).isNull();
        assertThat(dto.originalName()).isEqualTo("TEST");
        assertThat(dto.cleanName()).isEqualTo("Test");
    }
}
