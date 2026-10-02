package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** The request payload for updating an existing {@code Transaction}, identified by {@code id}. */
@Getter
@Builder(toBuilder = true)
@ToString
public class TransactionUpdateRequest {

    @NotNull(message = "Transaction ID cannot be null.")
    @Positive(message = "Transaction ID must be a positive number.")
    private final Long id;

    @NotNull(message = "Account ID cannot be null.")
    @Positive(message = "Account ID must be a positive number.")
    private final Long accountId;

    @Positive(message = "Category ID must be a positive number.")
    private final Long categoryId;

    @NotNull(message = "Starting balance cannot be null.")
    @DecimalMin(value = "-9999999999.99", message = "transaction amount must be greater than or equal to -9999999999.99")
    @DecimalMax(value = "9999999999.99", message = "transaction amount must be less than or equal to 9999999999.99")
    private final BigDecimal amount;

    @NotNull(message = "Transaction date cannot be null.")
    private final OffsetDateTime transactionDate;

    @NotBlank(message = "Description cannot be blank.")
    @Size(max = 255, message = "Description must be less than 255 characters.")
    private final String description;

    @NotBlank(message = "Type cannot be blank.")
    @Size(max = 20, message = "Type must be less than 20 characters.")
    private final String type;

    private final OffsetDateTime postDate;

    @Positive(message = "Merchant ID must be a positive number.")
    private final Long merchantId;

    /**
     * Builds an update-request DTO from an existing domain {@code Transaction}. Currently unused
     * by any caller in this codebase -- kept as a documented extension point rather than removed,
     * since its natural use (pre-populating an edit form/request from a loaded transaction) is a
     * real future need, not speculative.
     *
     * @param transaction the domain transaction to convert
     * @return an equivalent update-request DTO
     */
    public static TransactionUpdateRequest fromDomain(Transaction transaction) {
        return TransactionUpdateRequest.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccount() != null ? transaction.getAccount().getId() : null)
                .categoryId(transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .description(transaction.getDescription())
                .type(transaction.getType().name())
                .postDate(transaction.getPostDate())
                .merchantId(transaction.getMerchant() != null ? transaction.getMerchant().getId() : null)
                .build();
    }

    /**
     * Batch form of {@link #fromDomain(Transaction)}.
     *
     * @param transactions the domain transactions to convert
     * @return the equivalent update-request DTOs, in the same order
     */
    public static List<TransactionUpdateRequest> fromDomain(List<Transaction> transactions) {
        return transactions.stream()
                .map(TransactionUpdateRequest::fromDomain)
                .toList();
    }
}
