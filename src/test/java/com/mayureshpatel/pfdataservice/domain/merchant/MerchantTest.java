//package com.mayureshpatel.pfdataservice.domain.merchant;
//
//import com.mayureshpatel.pfdataservice.domain.user.User;
//import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
//import com.mayureshpatel.pfdataservice.mapper.MerchantDtoMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DisplayName("Merchant domain object tests")
//class MerchantTest {
//
//    @Test
//    @DisplayName("toDto — should map all fields correctly")
//    void toDto_mapsAllFields() {
//        // Arrange
//        User user = new User();
//        user.setId(1L);
//
//        Merchant merchant = new Merchant();
//        merchant.setId(10L);
//        merchant.setUser(user);
//        merchant.setOriginalName("AMZN MKTP");
//        merchant.setCleanName("Amazon");
//
//        // Act
//        MerchantDto dto = MerchantDtoMapper.toDto(merchant);
//
//        // Assert
//        assertThat(dto.id()).isEqualTo(10L);
//        assertThat(dto.originalName()).isEqualTo("AMZN MKTP");
//        assertThat(dto.cleanName()).isEqualTo("Amazon");
//    }
//}
