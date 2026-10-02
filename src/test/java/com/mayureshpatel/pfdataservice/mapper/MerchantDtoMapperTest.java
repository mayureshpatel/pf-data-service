package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code MerchantDtoMapper}'s static {@code toDto} mapping (in {@link ToDtoMappingTests}), plus confirms the private constructor of this static-utility-only class is instantiable via reflection, solely to satisfy coverage tooling. */
@DisplayName("MerchantDtoMapper Unit Tests")
class MerchantDtoMapperTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // arrange
        Constructor<MerchantDtoMapper> constructor = MerchantDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // act
        MerchantDtoMapper instance = constructor.newInstance();

        // assert & verify
        assertNotNull(instance);
    }

    /** {@code toDto} returns null for a null source, maps every field (post-PF-845's own name/city/state/postalCode/country shape) when fully populated, and leaves every location field null without throwing when the source leaves them unset. */
    @Nested
    @DisplayName("Method: toDto")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when source is null")
        void toDto_shouldReturnNullWhenSourceIsNull() {
            // act
            MerchantDto result = MerchantDtoMapper.toDto(null);

            // assert & verify
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // arrange
            Merchant merchant = Merchant.builder()
                    .id(1L)
                    .userId(100L)
                    .name("McDonald's")
                    .city("Atlanta")
                    .state("GA")
                    .postalCode("30301")
                    .country("USA")
                    .build();

            // act
            MerchantDto dto = MerchantDtoMapper.toDto(merchant);

            // assert & verify
            assertNotNull(dto);
            assertEquals(merchant.getId(), dto.id());
            assertEquals(merchant.getUserId(), dto.userId());
            assertEquals(merchant.getName(), dto.name());
            assertEquals(merchant.getCity(), dto.city());
            assertEquals(merchant.getState(), dto.state());
            assertEquals(merchant.getPostalCode(), dto.postalCode());
            assertEquals(merchant.getCountry(), dto.country());
        }

        @Test
        @DisplayName("should handle unset location fields")
        void toDto_shouldHandleUnsetLocationFields() {
            // arrange
            Merchant merchant = Merchant.builder()
                    .id(1L)
                    .userId(100L)
                    .name("Test")
                    .build();

            // act
            MerchantDto dto = MerchantDtoMapper.toDto(merchant);

            // assert & verify
            assertNotNull(dto);
            assertNull(dto.city());
            assertNull(dto.state());
            assertNull(dto.postalCode());
            assertNull(dto.country());
        }
    }
}
