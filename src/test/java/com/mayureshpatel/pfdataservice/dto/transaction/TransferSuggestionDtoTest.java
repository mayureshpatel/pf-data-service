package com.mayureshpatel.pfdataservice.dto.transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies {@code TransferSuggestionDto}'s record accessors map their constructor arguments 1:1. */
@DisplayName("TransferSuggestionDto Structure Tests")
class TransferSuggestionDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        TransactionDto source = TransactionDto.builder().id(1L).build();
        TransactionDto target = TransactionDto.builder().id(2L).build();
        double confidence = 0.95;

        TransferSuggestionDto dto = new TransferSuggestionDto(source, target, confidence);

        assertEquals(source, dto.sourceTransaction());
        assertEquals(target, dto.targetTransaction());
        assertEquals(confidence, dto.confidenceScore());
    }
}
