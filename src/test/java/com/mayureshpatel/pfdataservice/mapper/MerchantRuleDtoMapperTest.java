package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Verifies {@code MerchantRuleDtoMapper.toDto}: null in gives null out, every field maps (nesting the merchant), and a null user or merchant maps to null without throwing. */
@DisplayName("MerchantRuleDtoMapper Unit Tests")
class MerchantRuleDtoMapperTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // arrange
        Constructor<MerchantRuleDtoMapper> constructor = MerchantRuleDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // act
        MerchantRuleDtoMapper instance = constructor.newInstance();

        // assert & verify
        assertNotNull(instance);
    }

    @Test
    @DisplayName("should return null when source is null")
    void toDto_shouldReturnNullWhenSourceIsNull() {
        // act & assert & verify
        assertNull(MerchantRuleDtoMapper.toDto(null));
    }

    @Test
    @DisplayName("should map all fields when source is fully populated")
    void toDto_shouldMapAllFields() {
        // arrange
        MerchantRule rule = MerchantRule.builder()
                .id(1L)
                .user(User.builder().id(100L).build())
                .keywords(List.of("KROGER", "FUEL"))
                .matchType(MatchType.AND)
                .priority(4)
                .merchant(Merchant.builder().id(50L).userId(100L).name("Kroger").city("Atlanta").build())
                .minAmount(new BigDecimal("5.00"))
                .maxAmount(new BigDecimal("100.00"))
                .build();

        // act
        MerchantRuleDto dto = MerchantRuleDtoMapper.toDto(rule);

        // assert & verify
        assertEquals(1L, dto.id());
        assertEquals(100L, dto.userId());
        assertEquals(List.of("KROGER", "FUEL"), dto.keywords());
        assertEquals(MatchType.AND, dto.matchType());
        assertEquals(4, dto.priority());
        assertEquals(50L, dto.merchant().id());
        assertEquals("Kroger", dto.merchant().name());
        assertEquals("Atlanta", dto.merchant().city());
        assertEquals(new BigDecimal("5.00"), dto.minAmount());
        assertEquals(new BigDecimal("100.00"), dto.maxAmount());
    }

    @Test
    @DisplayName("should handle null user and merchant")
    void toDto_shouldHandleNulls() {
        // arrange
        MerchantRule rule = MerchantRule.builder().id(1L).keywords(List.of("TEST")).priority(10).build();

        // act
        MerchantRuleDto dto = MerchantRuleDtoMapper.toDto(rule);

        // assert & verify
        assertNull(dto.userId());
        assertNull(dto.merchant());
    }
}
