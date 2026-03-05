package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("TransactionDto Structure Tests")
class TransactionDtoTest {

    @Test
    @DisplayName("should correctly map all fields using constructor")
    void shouldPopulateFieldsViaConstructor() {
        AccountDto account = AccountDto.builder().id(1L).build();
        CategoryDto category = CategoryDto.builder().id(2L).build();
        MerchantDto merchant = MerchantDto.builder().id(3L).build();
        BigDecimal amount = new BigDecimal("100.00");
        OffsetDateTime date = OffsetDateTime.now();
        OffsetDateTime postDate = OffsetDateTime.now().plusDays(1);
        String description = "Test";
        TransactionType type = TransactionType.EXPENSE;

        TransactionDto dto = new TransactionDto(
                1L, account, category, amount, date, description, type, postDate, merchant
        );

        assertEquals(1L, dto.id());
        assertEquals(account, dto.account());
        assertEquals(category, dto.category());
        assertEquals(amount, dto.amount());
        assertEquals(date, dto.date());
        assertEquals(description, dto.description());
        assertEquals(type, dto.type());
        assertEquals(postDate, dto.postDate());
        assertEquals(merchant, dto.merchant());
    }

    @Test
    @DisplayName("should correctly map all fields using builder")
    void shouldPopulateFieldsViaBuilder() {
        AccountDto account = AccountDto.builder().id(1L).build();
        CategoryDto category = CategoryDto.builder().id(2L).build();
        MerchantDto merchant = MerchantDto.builder().id(3L).build();
        BigDecimal amount = new BigDecimal("100.00");
        OffsetDateTime date = OffsetDateTime.now();
        OffsetDateTime postDate = OffsetDateTime.now().plusDays(1);
        String description = "Test";
        TransactionType type = TransactionType.EXPENSE;

        TransactionDto dto = TransactionDto.builder()
                .id(1L)
                .account(account)
                .category(category)
                .amount(amount)
                .date(date)
                .description(description)
                .type(type)
                .postDate(postDate)
                .merchant(merchant)
                .build();

        assertEquals(1L, dto.id());
        assertEquals(account, dto.account());
        assertEquals(category, dto.category());
        assertEquals(amount, dto.amount());
        assertEquals(date, dto.date());
        assertEquals(description, dto.description());
        assertEquals(type, dto.type());
        assertEquals(postDate, dto.postDate());
        assertEquals(merchant, dto.merchant());
    }
}
