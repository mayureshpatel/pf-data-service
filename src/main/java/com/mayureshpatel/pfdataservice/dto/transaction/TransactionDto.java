package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record TransactionDto(
        Long id,
        OffsetDateTime date,
        OffsetDateTime postDate,
        String description,
        MerchantDto merchant,
        BigDecimal amount,
        TransactionType type,
        Category category,
        Long accountId
) {
}