package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.exception.DuplicateImportException;
import com.mayureshpatel.pfdataservice.repository.file_import_history.FileImportHistoryRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Duplicate detection for CSV imports, at both levels an import can repeat itself:
 * <ul>
 *   <li>whole-file: a file hash already in the account's import history (checked before a save
 *       and by the Review step's import-check, and recorded after a successful save), and</li>
 *   <li>row-level: each row's (date, amount, description, type) signature, compared against
 *       already-saved transactions in the batch's date range and against earlier rows of the
 *       same batch.</li>
 * </ul>
 * Extracted from {@link TransactionImportService} (PF-898) once its per-file results, merchant
 * review, and import-check work pushed it over PMD's {@code GodClass} threshold. Duplicate
 * detection is the one self-contained concern there, and keeping the single signature definition
 * and the single history lookup here guarantees preview, import-check, and save can never disagree
 * about what counts as a duplicate.
 */
@Component
@Slf4j
public class ImportDuplicateDetector {

    private final TransactionRepository transactionRepository;
    private final FileImportHistoryRepository fileImportHistoryRepository;

    /**
     * Creates the detector.
     *
     * @param transactionRepository       the repository used to load already-saved transactions
     *                                    for the signature lookup
     * @param fileImportHistoryRepository the import history used for whole-file duplicate detection
     */
    public ImportDuplicateDetector(TransactionRepository transactionRepository,
                                   FileImportHistoryRepository fileImportHistoryRepository) {
        this.transactionRepository = transactionRepository;
        this.fileImportHistoryRepository = fileImportHistoryRepository;
    }

    /**
     * Whether a file with this hash was already imported into the account.
     *
     * @param accountId the account id
     * @param fileHash  the file's hash
     * @return {@code true} if the import history already holds this (account, hash) pair
     */
    public boolean isAlreadyImported(Long accountId, String fileHash) {
        return fileImportHistoryRepository.findByAccountIdAndFileHash(accountId, fileHash).isPresent();
    }

    /**
     * Rejects the whole batch outright if its file hash matches a previously imported file. A
     * batch with no hash is never rejected.
     *
     * @param accountId the account the batch is being saved to
     * @param fileHash  the source file's hash (optional)
     * @throws DuplicateImportException if this exact file was already imported
     */
    public void requireNotAlreadyImported(Long accountId, String fileHash) {
        if (fileHash != null && isAlreadyImported(accountId, fileHash)) {
            log.warn("Duplicate file hash detected during save. Account ID: {}, Hash: {}", accountId, fileHash);
            throw new DuplicateImportException("This file has already been imported.");
        }
    }

    /**
     * Records a successful import in the history so the same file is rejected next time. Skipped
     * when either the filename or the hash is missing, since a hashless batch can't be matched.
     *
     * @param account          the account the file was imported into
     * @param fileName         the source filename (optional)
     * @param fileHash         the source file's hash (optional)
     * @param transactionCount how many transactions the import saved
     */
    public void recordImport(Account account, String fileName, String fileHash, int transactionCount) {
        if (fileName != null && fileHash != null) {
            FileImportHistory history = FileImportHistory.builder()
                    .account(account)
                    .fileName(fileName)
                    .fileHash(fileHash)
                    .transactionCount(transactionCount)
                    .build();
            fileImportHistoryRepository.save(history);
        }
    }

    /**
     * The transactions actually ready to insert, plus how many were skipped as duplicates of
     * either an existing row or an earlier row in this same batch.
     *
     * @param uniqueTransactions the rows to insert, already mapped to create requests
     * @param duplicateCount     the number of rows skipped as duplicates
     */
    public record DedupeResult(List<TransactionCreateRequest> uniqueTransactions, int duplicateCount) {
    }

    /**
     * The single definition of a transaction's duplicate-detection signature, used by both
     * preview and save so the two can never disagree about what counts as a duplicate. The amount
     * is stripped of trailing zeros so {@code 5.0} and {@code 5.00} compare equal.
     *
     * @param date        the transaction date
     * @param amount      the transaction amount
     * @param description the transaction description
     * @param type        the transaction type (may be null)
     * @return the signature string
     */
    public static String buildSignature(OffsetDateTime date, BigDecimal amount, String description, TransactionType type) {
        return String.format("%s_%s_%s_%s", date, amount.stripTrailingZeros(), description, type == null ? null : type.name());
    }

    /**
     * The signatures of every already-saved transaction on the account within the date range
     * spanned by the given dates, to check incoming rows against. An empty date list falls back to
     * a range of "now".
     *
     * @param accountId the account the rows are being imported into
     * @param dates     the incoming rows' dates
     * @return the existing transactions' signatures
     */
    public Set<String> findExistingSignatures(Long accountId, List<OffsetDateTime> dates) {
        OffsetDateTime earliest = dates.stream().min(OffsetDateTime::compareTo).orElse(OffsetDateTime.now());
        OffsetDateTime latest = dates.stream().max(OffsetDateTime::compareTo).orElse(OffsetDateTime.now());

        List<Transaction> existingTransactions = transactionRepository.findExistingForDuplicateCheck(accountId, earliest, latest);

        return existingTransactions.stream()
                .map(t -> buildSignature(t.getTransactionDate(), t.getAmount(), t.getDescription(), t.getType()))
                .collect(Collectors.toSet());
    }

    /**
     * Splits an approved batch into the rows to insert and a count of the rows skipped because
     * their signature matches an existing transaction or an earlier row of the same batch. Each
     * kept row is mapped to a create request for the given account, with its merchant taken from
     * the row itself when set, otherwise from the description match map.
     *
     * @param approvedDtos       the approved rows, in batch order
     * @param existingSignatures the signatures of already-saved transactions
     * @param account            the account the rows are being saved to
     * @param merchantMap        description to merchant id matches (empty for a reviewed batch)
     * @return the rows to insert and the duplicate count
     */
    public DedupeResult deduplicate(List<TransactionDto> approvedDtos, Set<String> existingSignatures, Account account,
                                    Map<String, Long> merchantMap) {
        List<TransactionCreateRequest> uniqueTransactions = new ArrayList<>();
        Set<String> batchSignatures = new HashSet<>();
        int duplicateCount = 0;

        for (TransactionDto dto : approvedDtos) {
            String signature = buildSignature(dto.date(), dto.amount(), dto.description(), dto.type());

            boolean existsInDb = existingSignatures.contains(signature);
            boolean existsInBatch = batchSignatures.contains(signature);

            if (!existsInDb && !existsInBatch) {
                // a row's own reviewed merchant (PF-898, ownership already verified) wins over the
                // description match; a reviewed batch passes an empty match map, so a cleared
                // merchant stays cleared. a description with no existing link (PF-845) just leaves
                // merchantId null -- the expected outcome for anything nobody has linked yet.
                // import never creates a merchant or a link, and an override never teaches one.
                Long merchantId = dto.merchant() != null && dto.merchant().id() != null
                        ? dto.merchant().id()
                        : merchantMap.get(dto.description());
                TransactionCreateRequest t = mapToEntity(dto)
                        .toBuilder()
                        .accountId(account.getId())
                        .merchantId(merchantId)
                        .build();
                uniqueTransactions.add(t);
                batchSignatures.add(signature);
            } else {
                duplicateCount++;
                if (log.isTraceEnabled()) {
                    log.trace("Skipping duplicate transaction: {} - {} - {}", dto.date(), dto.description(), dto.amount());
                }
            }
        }

        return new DedupeResult(uniqueTransactions, duplicateCount);
    }

    /**
     * Converts a previewed transaction back into the request shape the repository layer expects.
     *
     * @param dto the previewed transaction
     * @return the equivalent create request
     */
    private TransactionCreateRequest mapToEntity(TransactionDto dto) {
        return TransactionCreateRequest.builder()
                .transactionDate(dto.date())
                .postDate(dto.postDate())
                .description(dto.description())
                .amount(dto.amount())
                .type(dto.type().name())
                .categoryId(dto.category() != null ? dto.category().id() : null)
                .build();
    }
}
