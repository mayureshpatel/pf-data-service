package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;

public final class TransactionDtoMapper {

    private TransactionDtoMapper() {}

    public static TransactionDto toDto(Transaction transaction) {
        if (transaction == null) return null;
        return new TransactionDto(
                transaction.getId(),
                transaction.getTransactionDate(),
                transaction.getPostDate(),
                transaction.getDescription(),
                MerchantDtoMapper.toDto(transaction.getMerchant()),
                transaction.getAmount(),
                transaction.getType(),
                CategoryDtoMapper.toDto(transaction.getCategory()),
                AccountDtoMapper.toDto(transaction.getAccount())
        );
    }
}
