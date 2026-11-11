package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies the net-change sign rule for the contribution and earnings types, on both the persisted {@link Transaction} and the {@link TransactionCreateRequest}. */
@DisplayName("TransactionType net-change rules")
class TransactionTypeTest {

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"CONTRIBUTION", "EMPLOYER_CONTRIBUTION", "EARNINGS"})
    @DisplayName("Transaction.getNetChange is +abs for contribution and earnings types")
    void transactionNetChange_shouldBePositiveAbs(TransactionType type) {
        // arrange
        Transaction negative = Transaction.builder().type(type).amount(new BigDecimal("-40.00")).build();
        Transaction positive = Transaction.builder().type(type).amount(new BigDecimal("40.00")).build();

        // act & assert & verify
        assertEquals(new BigDecimal("40.00"), negative.getNetChange());
        assertEquals(new BigDecimal("40.00"), positive.getNetChange());
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"CONTRIBUTION", "EMPLOYER_CONTRIBUTION", "EARNINGS"})
    @DisplayName("TransactionCreateRequest.getNetChange is +abs for contribution and earnings types")
    void requestNetChange_shouldBePositiveAbs(TransactionType type) {
        // arrange
        TransactionCreateRequest request = TransactionCreateRequest.builder()
                .type(type.name()).amount(new BigDecimal("-40.00")).build();

        // act & assert & verify
        assertEquals(new BigDecimal("40.00"), request.getNetChange());
    }
}
