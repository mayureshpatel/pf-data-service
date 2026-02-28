package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
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
        AccountDto account
) {

    /**
     * Maps a {@link Transaction} domain object to its corresponding DTO representation.
     *
     * @param transaction The Transaction domain object to be mapped.
     * @return The {@link TransactionDto} representation of the provided Transaction.
     */
    public static TransactionDto mapToDto(Transaction transaction) {
        return new TransactionDto(
                transaction.getId(),
                transaction.getTransactionDate(),
                transaction.getPostDate(),
                transaction.getDescription(),
                MerchantDto.mapToDto(transaction.getMerchant()),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory(),
                AccountDto.fromDomain(transaction.getAccount())
        );
    }
}