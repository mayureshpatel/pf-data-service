//package com.mayureshpatel.pfdataservice.dto.transaction;
//
//import jakarta.validation.ConstraintViolation;
//import jakarta.validation.Validation;
//import jakarta.validation.Validator;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Set;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DisplayName("SaveTransactionRequest validation tests")
//class SaveTransactionRequestValidationTest {
//
//    private static Validator validator;
//
//    @BeforeAll
//    static void setUp() {
//        validator = Validation.buildDefaultValidatorFactory().getValidator();
//    }
//
//    @Test
//    @DisplayName("should pass validation with non-empty transactions list")
//    void validate_nonEmptyList_noViolations() {
//        TransactionDto tx = TransactionDto.builder()
//                .id(1L)
//                .date(java.time.OffsetDateTime.now())
//                .description("Test")
//                .amount(java.math.BigDecimal.TEN)
//                .type(com.mayureshpatel.pfdataservice.domain.transaction.TransactionType.EXPENSE)
//                .account(new com.mayureshpatel.pfdataservice.dto.account.AccountDto(1L, 1L, "Checking", "C", "Checking", java.math.BigDecimal.ZERO, "USD", "$", "Bank"))
//                .build();
//        SaveTransactionRequest request = new SaveTransactionRequest(List.of(tx), "file.csv", "hash123");
//
//        Set<ConstraintViolation<SaveTransactionRequest>> violations = validator.validate(request);
//
//        assertThat(violations).isEmpty();
//    }
//
//    @Test
//    @DisplayName("should fail when transactions list is empty")
//    void validate_emptyList_hasViolation() {
//        SaveTransactionRequest request = new SaveTransactionRequest(Collections.emptyList(), "file.csv", "hash123");
//
//        Set<ConstraintViolation<SaveTransactionRequest>> violations = validator.validate(request);
//
//        assertThat(violations).anyMatch(v ->
//                v.getPropertyPath().toString().equals("transactions") &&
//                        v.getMessage().contains("must not be empty"));
//    }
//
//    @Test
//    @DisplayName("should fail when transactions list is null")
//    void validate_nullList_hasViolation() {
//        SaveTransactionRequest request = new SaveTransactionRequest(null, "file.csv", "hash123");
//
//        Set<ConstraintViolation<SaveTransactionRequest>> violations = validator.validate(request);
//
//        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("transactions"));
//    }
//}
