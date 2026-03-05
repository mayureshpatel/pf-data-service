package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("TransactionPreviewDto Structure Tests")
class TransactionPreviewDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        OffsetDateTime date = OffsetDateTime.now();
        OffsetDateTime postDate = OffsetDateTime.now().plusDays(1);
        String description = "Test";
        BigDecimal amount = new BigDecimal("50.00");
        TransactionType type = TransactionType.EXPENSE;
        CategoryDto suggestedCategory = CategoryDto.builder().id(2L).build();
        MerchantDto suggestedMerchant = MerchantDto.builder().id(3L).build();

        TransactionPreviewDto dto = TransactionPreviewDto.builder()
                .date(date)
                .postDate(postDate)
                .description(description)
                .amount(amount)
                .type(type)
                .suggestedCategory(suggestedCategory)
                .suggestedMerchant(suggestedMerchant)
                .build();

        assertEquals(date, dto.date());
        assertEquals(postDate, dto.postDate());
        assertEquals(description, dto.description());
        assertEquals(amount, dto.amount());
        assertEquals(type, dto.type());
        assertEquals(suggestedCategory, dto.suggestedCategory());
        assertEquals(suggestedMerchant, dto.suggestedMerchant());
    }
}
