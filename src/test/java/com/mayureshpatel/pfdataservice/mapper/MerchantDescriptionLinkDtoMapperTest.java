package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.MerchantDescriptionLink;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MerchantDescriptionLinkDtoMapper Unit Tests")
class MerchantDescriptionLinkDtoMapperTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // arrange
        Constructor<MerchantDescriptionLinkDtoMapper> constructor = MerchantDescriptionLinkDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // act
        MerchantDescriptionLinkDtoMapper instance = constructor.newInstance();

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
            MerchantDescriptionLinkDto result = MerchantDescriptionLinkDtoMapper.toDto(null);

            // assert & verify
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // arrange
            MerchantDescriptionLink link = MerchantDescriptionLink.builder()
                    .id(1L)
                    .userId(100L)
                    .merchantId(7L)
                    .description("STARBUCKS #1")
                    .normalizedDescription("starbucks #1")
                    .build();

            // act
            MerchantDescriptionLinkDto dto = MerchantDescriptionLinkDtoMapper.toDto(link);

            // assert & verify
            assertNotNull(dto);
            assertEquals(link.getId(), dto.id());
            assertEquals(link.getMerchantId(), dto.merchantId());
            assertEquals(link.getDescription(), dto.description());
        }
    }
}
