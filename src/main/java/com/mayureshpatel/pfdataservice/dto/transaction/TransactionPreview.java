package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record TransactionPreview(
        OffsetDateTime date,
        OffsetDateTime postDate,
        String description,
        BigDecimal amount,
        TransactionType type,
        Category suggestedCategory,
        MerchantDto suggestedMerchant
) {
}
