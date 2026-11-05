package com.mayureshpatel.pfdataservice.dto.merchant;

import com.mayureshpatel.pfdataservice.dto.ValidatorTestBase;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies {@link MerchantRuleCreateRequest}'s bean-validation constraints: a valid request passes, and merchant id, keywords (at least one, at most 20, each non-blank and at most 255 characters), priority and the amount range fail correctly. */
@DisplayName("MerchantRuleCreateRequest Validation Tests")
class MerchantRuleCreateRequestTest extends ValidatorTestBase {

    private static final String KEYWORD = "KROGER";

    private MerchantRuleCreateRequest.MerchantRuleCreateRequestBuilder valid() {
        return MerchantRuleCreateRequest.builder()
                .merchantId(1L)
                .keywords(List.of(KEYWORD));
    }

    private Set<String> messages(MerchantRuleCreateRequest request) {
        return validator().validate(request).stream().map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("should pass when all fields are valid, with and without the optional fields")
    void shouldPassWithValidData() {
        // act & assert & verify
        assertTrue(validator().validate(valid().build()).isEmpty());
        assertTrue(validator().validate(valid().priority(3).minAmount(new BigDecimal("5.00"))
                .maxAmount(new BigDecimal("5.00")).build()).isEmpty());
    }

    @Test
    @DisplayName("should fail when merchantId is null or not positive")
    void shouldFailOnBadMerchantId() {
        // act & assert & verify
        assertTrue(messages(valid().merchantId(null).build()).contains("Merchant ID cannot be null."));
        assertTrue(messages(valid().merchantId(0L).build()).contains("Merchant ID must be a positive number."));
    }

    @Test
    @DisplayName("should fail when keywords is empty, has a blank entry or an entry over 255 characters")
    void shouldFailOnBadKeywords() {
        // act & assert & verify
        assertTrue(messages(valid().keywords(List.of()).build()).contains("At least one keyword is required."));
        assertTrue(messages(valid().keywords(List.of(KEYWORD, " ")).build()).contains("Keyword cannot be blank."));
        assertTrue(messages(valid().keywords(List.of("a".repeat(256))).build())
                .contains("Keyword cannot exceed 255 characters."));
    }

    @Test
    @DisplayName("should allow exactly 20 keywords and fail with 21")
    void shouldCapKeywordsAtTwenty() {
        // arrange
        List<String> twenty = IntStream.rangeClosed(1, 20).mapToObj(i -> "KW" + i).toList();
        List<String> twentyOne = IntStream.rangeClosed(1, 21).mapToObj(i -> "KW" + i).toList();

        // act & assert & verify
        assertTrue(validator().validate(valid().keywords(twenty).build()).isEmpty());
        assertTrue(messages(valid().keywords(twentyOne).build()).contains("A rule can have at most 20 keywords."));
    }

    @Test
    @DisplayName("should fail when priority or an amount bound is negative")
    void shouldFailOnNegativeNumbers() {
        // act & assert & verify
        assertTrue(messages(valid().priority(-1).build()).contains("Priority must be a positive number or zero."));
        assertTrue(messages(valid().minAmount(new BigDecimal("-0.01")).build())
                .contains("Minimum amount must be a positive number or zero."));
        assertTrue(messages(valid().maxAmount(new BigDecimal("-0.01")).build())
                .contains("Maximum amount must be a positive number or zero."));
    }

    @Test
    @DisplayName("should fail when the minimum amount exceeds the maximum, and pass when only one bound is set")
    void shouldCheckAmountRangeConsistency() {
        // act & assert & verify
        Set<String> inconsistent = messages(valid().minAmount(new BigDecimal("50.00"))
                .maxAmount(new BigDecimal("10.00")).build());
        assertEquals(Set.of("Minimum amount cannot exceed maximum amount."), inconsistent);
        assertTrue(validator().validate(valid().minAmount(new BigDecimal("50.00")).build()).isEmpty());
        assertTrue(validator().validate(valid().maxAmount(new BigDecimal("10.00")).build()).isEmpty());
    }
}
