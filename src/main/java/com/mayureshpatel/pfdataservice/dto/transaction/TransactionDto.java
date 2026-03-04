package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record TransactionDto(
        Long id,
        AccountDto account,
        CategoryDto category,
        BigDecimal amount,
        OffsetDateTime date,
        String description,
        TransactionType type,
        OffsetDateTime postDate,
        MerchantDto merchant
) {
}