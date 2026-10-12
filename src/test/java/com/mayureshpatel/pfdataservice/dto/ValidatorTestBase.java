package com.mayureshpatel.pfdataservice.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Owns a single {@link ValidatorFactory}/{@link Validator} pair for an entire DTO validation test
 * class, closed once after all its tests run (PF-812) -- replacing every {@code *RequestTest}
 * class's own independent {@code Validation.buildDefaultValidatorFactory()} call, none of which
 * ever closed the factory they created. A {@code ValidatorFactory} is stateless and safe to reuse
 * across every test in a class, so sharing one here is a correctness fix for a real (if
 * low-stakes) resource leak, not just a style consolidation.
 */
public abstract class ValidatorTestBase {

    private static Validator validator;
    private static ValidatorFactory validatorFactory;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    /** @return the shared {@link Validator} for this test class's lifetime. */
    protected static Validator validator() {
        return validator;
    }
}
