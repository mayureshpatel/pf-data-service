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
        // Arrange
        Constructor<MerchantDtoMapper> constructor = MerchantDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act
        MerchantDtoMapper instance = constructor.newInstance();

        // Assert
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("Method: toDto")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when source is null")
        void toDto_shouldReturnNullWhenSourceIsNull() {
            // Act
            MerchantDto result = MerchantDtoMapper.toDto(null);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // Arrange
            Merchant merchant = Merchant.builder()
                    .id(1L)
                    .userId(100L)
                    .originalName("MCDONALDS 12345")
                    .cleanName("McDonald's")
                    .build();

            // Act
            MerchantDto dto = MerchantDtoMapper.toDto(merchant);

            // Assert
            assertNotNull(dto);
            assertEquals(merchant.getId(), dto.id());
            assertEquals(merchant.getUserId(), dto.userId());
            assertEquals(merchant.getOriginalName(), dto.originalName());
            assertEquals(merchant.getCleanName(), dto.cleanName());
        }

        @Test
        @DisplayName("should handle null userId")
        void toDto_shouldHandleNullUserId() {
            // Arrange
            Merchant merchant = Merchant.builder()
                    .id(1L)
                    .originalName("TEST")
                    .cleanName("Test")
                    .userId(null)
                    .build();

            // Act
            MerchantDto dto = MerchantDtoMapper.toDto(merchant);

            // Assert
            assertNotNull(dto);
            assertNull(dto.userId());
        }
    }
}
