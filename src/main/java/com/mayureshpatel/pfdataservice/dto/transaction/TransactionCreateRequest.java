package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * The request payload for creating a new {@code Transaction}. {@code type} is a plain string
 * rather than {@link com.mayureshpatel.pfdataservice.domain.transaction.TransactionType} at this
 * layer -- {@code TransactionService} converts it via {@code TransactionType.valueOf(...)}, whose
 * {@code IllegalArgumentException} on an unrecognized value is handled generically by
 * {@code GlobalExceptionHandler} rather than surfacing as a raw deserialization error. Note
 * {@link #getNetChange()} below does its own independent string comparison against the enum's
 * names and silently falls through to expense-like (negative) sign behavior for any value it
 * doesn't recognize, rather than throwing -- only {@code TransactionService}'s explicit
 * {@code valueOf} call actually rejects a bad type.
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class TransactionCreateRequest {

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
     * Calculates the net change this transaction applies to an account balance.
     * INCOME/TRANSFER_IN is positive, EXPENSE/TRANSFER_OUT is negative.
     *
     * @return the net change
     */
    public BigDecimal getNetChange() {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        if (TransactionType.ADJUSTMENT.name().equals(type)) {
            return amount;
        }

        if (TransactionType.INCOME.name().equals(type) || TransactionType.TRANSFER_IN.name().equals(type)) {
            return amount.abs();
        }

        return amount.abs().negate();
    }
}
