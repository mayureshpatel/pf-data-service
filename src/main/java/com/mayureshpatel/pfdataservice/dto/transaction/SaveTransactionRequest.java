package com.mayureshpatel.pfdataservice.dto.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * One previewed file's transactions to commit, plus the metadata the import pipeline uses for
 * whole-file duplicate detection.
 *
 * @param transactions      the previewed rows to save
 * @param fileName          the source filename, recorded in the import history
 * @param fileHash          the source file's hash, used to detect a re-uploaded file
 * @param accountId         the account to save into (used by the bulk endpoint)
 * @param merchantsReviewed {@code true} when every row's merchant was reviewed by the user, so a
 *                          row with no merchant means "no merchant" and the description-matched
 *                          merchant must not be filled back in (PF-898); {@code false} (the default
 *                          when omitted) keeps the legacy fallback to the description match
 */
public record SaveTransactionRequest(
        @NotEmpty(message = "Transactions list must not be empty")
        List<@Valid TransactionDto> transactions,

        @NotBlank(message = "File name cannot be blank")
        String fileName,

        @NotBlank(message = "File hash cannot be blank")
        String fileHash,

        Long accountId,

        boolean merchantsReviewed
) {

    /**
     * The legacy request shape without {@code merchantsReviewed}, which keeps the
     * description-match merchant fallback for rows that carry no merchant.
     *
     * @param transactions the previewed rows to save
     * @param fileName     the source filename
     * @param fileHash     the source file's hash
     * @param accountId    the account to save into
     */
    public SaveTransactionRequest(List<TransactionDto> transactions, String fileName, String fileHash, Long accountId) {
        this(transactions, fileName, fileHash, accountId, false);
    }
}
