package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record TransactionDto(
        Long id,
        
        @NotNull(message = "Transaction date is required")
        OffsetDateTime date,
        
        OffsetDateTime postDate,
        
        @Size(max = 255, message = "Description must be less than 255 characters")
        String description,
        
        MerchantDto merchant,
        
        @NotNull(message = "Amount is required")
        BigDecimal amount,
        
        @NotNull(message = "Transaction type is required")
        TransactionType type,
        
        CategoryDto category,
        
        @NotNull(message = "Account is required")
        AccountDto account
) {
}