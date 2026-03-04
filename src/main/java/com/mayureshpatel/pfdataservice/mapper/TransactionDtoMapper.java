package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;

public final class TransactionDtoMapper {

    private TransactionDtoMapper() {}

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
                MerchantDtoMapper.toDto(transaction.getMerchant())
        );
    }
}
