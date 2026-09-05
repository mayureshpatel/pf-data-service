package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;

import java.util.List;

public final class TransactionDtoMapper {

    private TransactionDtoMapper() {
    }

    public static TransactionDto toDto(Transaction transaction) {
        if (transaction == null) return null;
        return new TransactionDto(
                transaction.getId(),
                AccountDtoMapper.toDto(transaction.getAccount()),
                CategoryDtoMapper.toDto(transaction.getCategory()),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getDescription(),
                transaction.getType(),
                transaction.getPostDate(),
                MerchantDtoMapper.toDto(transaction.getMerchant()),
                transaction.getTags() != null
                        ? transaction.getTags().stream().map(TagDtoMapper::toDto).toList()
                        : List.of()
        );
    }
}
