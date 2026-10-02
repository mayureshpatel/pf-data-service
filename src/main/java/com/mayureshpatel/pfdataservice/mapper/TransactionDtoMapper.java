package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;

import java.util.List;

/** Converts the {@link Transaction} domain object into its API-facing {@link TransactionDto}. */
public final class TransactionDtoMapper {

    private TransactionDtoMapper() {
    }

    /**
     * Maps a {@code null} {@code tags} list to an empty list rather than {@code null} in the DTO
     * -- {@code tags} is only populated by the paginated list query path (see
     * {@link Transaction#getTags()}'s own doc), so this keeps every other caller from needing its
     * own null-check.
     *
     * @param transaction the domain transaction to convert, or {@code null}
     * @return the equivalent DTO, or {@code null} if {@code transaction} was {@code null}
     */
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
