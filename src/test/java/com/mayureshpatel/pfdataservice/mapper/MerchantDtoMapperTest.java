package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

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
                    .originalName("MCDONALDS 12345")
                    .cleanName("McDonald's")
                    .build();

            // act
            MerchantDto dto = MerchantDtoMapper.toDto(merchant);

            // assert & verify
            assertNotNull(dto);
            assertEquals(merchant.getId(), dto.id());
            assertEquals(merchant.getUserId(), dto.userId());
            assertEquals(merchant.getOriginalName(), dto.originalName());
            assertEquals(merchant.getCleanName(), dto.cleanName());
        }

        @Test
        @DisplayName("should handle null userId")
        void toDto_shouldHandleNullUserId() {
            // arrange
            Merchant merchant = Merchant.builder()
                    .id(1L)
                    .originalName("TEST")
                    .cleanName("Test")
                    .userId(null)
                    .build();

            // act
            MerchantDto dto = MerchantDtoMapper.toDto(merchant);

            // assert & verify
            assertNotNull(dto);
            assertNull(dto.userId());
        }
    }
}
