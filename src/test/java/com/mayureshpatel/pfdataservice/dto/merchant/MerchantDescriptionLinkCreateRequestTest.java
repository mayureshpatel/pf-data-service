package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MerchantDescriptionLinkCreateRequest Validation Tests")
class MerchantDescriptionLinkCreateRequestTest {

    private Validator validator;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass validation with a valid description")
    void shouldPassWithValidDescription() {
        MerchantDescriptionLinkCreateRequest request = new MerchantDescriptionLinkCreateRequest("STARBUCKS #1");

        Set<ConstraintViolation<MerchantDescriptionLinkCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("should fail when description is blank")
    void shouldFailWhenDescriptionIsBlank() {
        MerchantDescriptionLinkCreateRequest request = new MerchantDescriptionLinkCreateRequest("");

        Set<ConstraintViolation<MerchantDescriptionLinkCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Description cannot be blank.")));
    }

    @Test
    @DisplayName("should fail when description exceeds 255 characters")
    void shouldFailWhenDescriptionIsTooLong() {
        MerchantDescriptionLinkCreateRequest request = new MerchantDescriptionLinkCreateRequest("A".repeat(256));

        Set<ConstraintViolation<MerchantDescriptionLinkCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Description must be less than 255 characters.")));
    }

    @Test
    @DisplayName("should expose the description via the record accessor")
    void shouldExposeDescription() {
        MerchantDescriptionLinkCreateRequest request = new MerchantDescriptionLinkCreateRequest("STARBUCKS #1");
        assertEquals("STARBUCKS #1", request.description());
    }
}
