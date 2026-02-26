package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;

public record CategoryTransactionsDto(
        CategoryDto category,
        Integer transactionCount
) {
}
