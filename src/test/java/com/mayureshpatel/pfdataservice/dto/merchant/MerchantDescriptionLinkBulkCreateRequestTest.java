package com.mayureshpatel.pfdataservice.dto.merchant;

import com.mayureshpatel.pfdataservice.dto.ValidatorTestBase;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies {@code MerchantDescriptionLinkBulkCreateRequest}: a non-empty list of at most 500 descriptions, each non-blank and no more than 255 characters. */
@DisplayName("MerchantDescriptionLinkBulkCreateRequest Validation Tests")
class MerchantDescriptionLinkBulkCreateRequestTest extends ValidatorTestBase {

    @Test
    @DisplayName("should pass validation with valid descriptions")
    void shouldPassWithValidDescriptions() {
        // arrange
        MerchantDescriptionLinkBulkCreateRequest request = new MerchantDescriptionLinkBulkCreateRequest(List.of("STARBUCKS #1", "SHELL OIL"));

        // act
        Set<ConstraintViolation<MerchantDescriptionLinkBulkCreateRequest>> violations = validator().validate(request);

        // assert & verify
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("should fail when the list is empty")
    void shouldFailWhenEmpty() {
        // arrange
        MerchantDescriptionLinkBulkCreateRequest request = new MerchantDescriptionLinkBulkCreateRequest(List.of());

        // act
        Set<ConstraintViolation<MerchantDescriptionLinkBulkCreateRequest>> violations = validator().validate(request);

        // assert & verify
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("should fail when more than 500 descriptions are sent")
    void shouldFailWhenTooMany() {
        // arrange
        MerchantDescriptionLinkBulkCreateRequest request = new MerchantDescriptionLinkBulkCreateRequest(Collections.nCopies(501, "A"));

        // act
        Set<ConstraintViolation<MerchantDescriptionLinkBulkCreateRequest>> violations = validator().validate(request);

        // assert & verify
        assertTrue(violations.stream().anyMatch(v -> "No more than 500 descriptions can be linked at once.".equals(v.getMessage())));
    }

    @Test
    @DisplayName("should fail when an entry is blank or longer than 255 characters")
    void shouldFailWhenEntryInvalid() {
        // arrange
        MerchantDescriptionLinkBulkCreateRequest blank = new MerchantDescriptionLinkBulkCreateRequest(List.of("OK", " "));
        MerchantDescriptionLinkBulkCreateRequest tooLong = new MerchantDescriptionLinkBulkCreateRequest(List.of("A".repeat(256)));

        // act & assert & verify
        assertFalse(validator().validate(blank).isEmpty());
        assertFalse(validator().validate(tooLong).isEmpty());
    }
}
