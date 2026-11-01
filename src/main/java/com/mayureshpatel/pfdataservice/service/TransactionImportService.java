package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.FileImportResultDto;
import com.mayureshpatel.pfdataservice.dto.transaction.FileImportStatus;
import com.mayureshpatel.pfdataservice.dto.transaction.ImportHashCheckRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionPreviewDto;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import com.mayureshpatel.pfdataservice.exception.DuplicateImportException;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper;
import com.mayureshpatel.pfdataservice.mapper.MerchantDtoMapper;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.file_import_history.FileImportHistoryRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParser;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses and saves bank CSV imports. {@link #previewTransactions} parses a file into a preview
 * (with a best-guess category per row) without touching the database; {@link #saveTransactions}
 * and {@link #saveBulkTransactions} commit a previously previewed batch, de-duplicating against
 * both already-saved transactions and the rest of the same batch by a
 * (date, amount, description, type) signature.
 */
@Service
@Slf4j
public class TransactionImportService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final FileImportHistoryRepository fileImportHistoryRepository;
    private final TransactionParserFactory parserFactory;
    private final TransactionCategorizer categorizer;
    private final CategoryRuleRepository categoryRuleRepository;
    private final MerchantService merchantService;
    private final AccountBalanceUpdateService accountBalanceUpdateService;
    private final TransactionImportService self;

    /**
     * The {@code @Lazy self} injection exists solely so {@link #saveBulkTransactions} can call
     * {@link #saveTransactions} through a Spring proxy rather than {@code this} -- a plain
     * {@code this.saveTransactions(...)} call bypasses Spring AOP entirely, so each request's own
     * {@code @Transactional} boundary would never actually apply.
     */
    @Autowired
    public TransactionImportService(TransactionRepository transactionRepository,
                                    AccountRepository accountRepository,
                                    CategoryRepository categoryRepository,
                                    FileImportHistoryRepository fileImportHistoryRepository,
                                    TransactionParserFactory parserFactory,
                                    TransactionCategorizer categorizer,
                                    CategoryRuleRepository categoryRuleRepository,
                                    MerchantService merchantService,
                                    AccountBalanceUpdateService accountBalanceUpdateService,
                                    @Lazy TransactionImportService self) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.fileImportHistoryRepository = fileImportHistoryRepository;
        this.parserFactory = parserFactory;
        this.categorizer = categorizer;
        this.categoryRuleRepository = categoryRuleRepository;
        this.merchantService = merchantService;
        this.accountBalanceUpdateService = accountBalanceUpdateService;
        this.self = self;
    }

    /**
     * Parses an uploaded file into a preview of the transactions it contains, with a best-guess
     * category applied to each row via the user's existing rules and categories, the merchant
     * its description is already linked to (if any), and a flag for rows that duplicate an
     * already-saved transaction or an earlier row of the same file. Nothing is saved or created.
     *
     * @param userId      the user id
     * @param accountId   the account the file will be imported into
     * @param bankName    the bank format to parse the file as
     * @param fileContent the file's contents
     * @param fileName    the original filename, used only for logging
     * @return the previewed transactions
     * @throws AccessDeniedException if the account belongs to a different user
     * @throws CsvParsingException   if the file can't be parsed
     */
    @Transactional(readOnly = true)
    public List<TransactionPreviewDto> previewTransactions(Long userId, Long accountId, String bankName, InputStream fileContent, String fileName) {
        log.info("Starting transaction preview for User: {}, Account ID: {}, Bank: {}, File: {}", userId, accountId, bankName, fileName);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        if (!account.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied to account");
        }

        TransactionParser parser = parserFactory.getTransactionParser(bankName);
        List<CategoryRule> userRules = this.categoryRuleRepository.findByUserId(userId);
        List<Category> userCategories = categoryRepository.findByUserId(userId);

        try (Stream<Transaction> rawTransactionStream = parser.parse(accountId, fileContent)) {
            List<Transaction> parsed = rawTransactionStream.toList();
            if (parsed.isEmpty()) {
                log.info("Generated 0 transaction previews (empty file)");
                return List.of();
            }

            // read-only lookups: an existing description link resolves to a merchant, nothing is created
            Map<String, Merchant> merchantsByDescription = merchantService.findMatchingMerchants(userId,
                    parsed.stream().map(Transaction::getDescription).toList());
            Set<String> existingSignatures = findExistingSignatures(accountId,
                    parsed.stream().map(Transaction::getTransactionDate).toList());
            Set<String> seenInFile = new HashSet<>();

            List<TransactionPreviewDto> previews = parsed.stream()
                    .map(t -> {
                        Long categoryId = categorizer.guessCategory(t, userRules, userCategories);
                        Category suggestedCategory = (categoryId != null && categoryId > 0)
                                ? userCategories.stream()
                                .filter(c -> c.getId().equals(categoryId))
                                .findFirst()
                                .orElse(null)
                                : null;
                        String signature = buildSignature(t.getTransactionDate(), t.getAmount(), t.getDescription(), t.getType());
                        // add() is false when an earlier row of this file already had the signature
                        boolean duplicate = existingSignatures.contains(signature) || !seenInFile.add(signature);
                        return TransactionPreviewDto.builder()
                                .date(t.getTransactionDate())
                                .postDate(t.getPostDate())
                                .description(t.getDescription())
                                .amount(t.getAmount())
                                .type(t.getType())
                                .suggestedCategory(CategoryDtoMapper.toDto(suggestedCategory))
                                .suggestedMerchant(MerchantDtoMapper.toDto(merchantsByDescription.get(t.getDescription())))
                                .duplicate(duplicate)
                                .build();
                    })
                    .toList();

            log.info("Generated {} transaction previews successfully", previews.size());
            return previews;
        } catch (Exception e) {
            log.error("Failed to process transaction preview for Account ID: {}", accountId, e);
            throw new CsvParsingException("Error processing transaction file", e);
        }
    }

    /**
     * The result of saving one batch: how many rows were inserted and how many were skipped as
     * duplicates of existing or same-batch rows.
     *
     * @param saved      the number of transactions actually saved
     * @param duplicates the number of rows skipped as duplicates
     */
    public record SaveOutcome(int saved, int duplicates) {
    }

    /**
     * Saves multiple previewed batches, possibly across different accounts, by calling
     * {@link #saveTransactions} once per request. Deliberately not {@code @Transactional}: each
     * call goes through {@code self} so it commits in its own transaction, which keeps one
     * already-imported or failing file from rolling back the others.
     *
     * @param userId   the user id
     * @param requests the batches to save
     * @return one result per request, in request order
     * @throws AccessDeniedException if any request targets an account owned by a different user
     */
    public List<FileImportResultDto> saveBulkTransactions(Long userId, List<SaveTransactionRequest> requests) {
        List<FileImportResultDto> results = new ArrayList<>();
        for (SaveTransactionRequest req : requests) {
            try {
                SaveOutcome outcome = self.saveTransactions(userId, req.accountId(), req.transactions(), req.fileName(), req.fileHash());
                results.add(new FileImportResultDto(req.fileName(), FileImportStatus.IMPORTED, outcome.saved(), outcome.duplicates(), null));
            } catch (DuplicateImportException e) {
                results.add(new FileImportResultDto(req.fileName(), FileImportStatus.SKIPPED_ALREADY_IMPORTED, 0, 0, null));
            } catch (AccessDeniedException e) {
                // ownership violations must surface as a 403, never as a per-file failure
                throw e;
            } catch (RuntimeException e) {
                log.warn("Bulk import failed for file: {}", req.fileName(), e);
                results.add(new FileImportResultDto(req.fileName(), FileImportStatus.FAILED, 0, 0, safeFailureMessage(e)));
            }
        }
        return results;
    }

    /**
     * Reports which (account, file hash) candidates were already imported, using the same history
     * lookup as the save path so the Review step can flag a file before the user commits. Each
     * distinct account is ownership-checked (and loaded) once.
     *
     * @param userId     the user id
     * @param candidates the (account, hash) pairs to check
     * @return the subset of candidates already in the import history, in request order
     * @throws AccessDeniedException     if any candidate targets an account owned by a different user
     * @throws ResourceNotFoundException if a candidate targets an account that does not exist
     */
    @Transactional(readOnly = true)
    public List<ImportHashCheckRequest> findAlreadyImported(Long userId, List<ImportHashCheckRequest> candidates) {
        Set<Long> verifiedAccountIds = new HashSet<>();
        List<ImportHashCheckRequest> alreadyImported = new ArrayList<>();
        for (ImportHashCheckRequest candidate : candidates) {
            // verify every account before touching history, so one bad account fails the whole call
            if (verifiedAccountIds.add(candidate.accountId())) {
                validateAccountOwnership(userId, candidate.accountId());
            }
        }
        for (ImportHashCheckRequest candidate : candidates) {
            if (fileImportHistoryRepository.findByAccountIdAndFileHash(candidate.accountId(), candidate.fileHash()).isPresent()) {
                alreadyImported.add(candidate);
            }
        }
        return alreadyImported;
    }

    /**
     * The client-safe message for a failed file: the exception's own message only for known
     * application exceptions (whose messages are written for users), otherwise a generic one so
     * database or framework internals never reach the client.
     */
    private String safeFailureMessage(RuntimeException e) {
        boolean knownAppException = e instanceof ResourceNotFoundException || e instanceof CsvParsingException;
        if (knownAppException && e.getMessage() != null) {
            return e.getMessage();
        }
        return "Could not import this file.";
    }

    /**
     * Saves a previewed batch of transactions to an account, skipping any that match either an
     * already-saved transaction or another row earlier in the same batch by a
     * (date, amount, description, type) signature. Also rejects the whole batch outright if its
     * file hash matches a previously imported file, when a hash is provided.
     *
     * @param userId       the user id
     * @param accountId    the account to save the transactions to
     * @param approvedDtos the previewed transactions to save
     * @param fileName     the source filename, recorded for duplicate-import history (optional)
     * @param fileHash     the source file's hash, used for whole-file duplicate detection (optional)
     * @return how many transactions were saved and how many were skipped as duplicates
     * @throws AccessDeniedException      if the account belongs to a different user
     * @throws DuplicateImportException   if this exact file was already imported
     */
    @Transactional
    public SaveOutcome saveTransactions(Long userId, Long accountId, List<TransactionDto> approvedDtos, String fileName, String fileHash) {
        log.info("Saving {} transactions for User: {}, Account ID: {}", approvedDtos.size(), userId, accountId);

        Account account = validateAccountOwnership(userId, accountId);
        checkForDuplicateFileImport(accountId, fileHash);

        if (approvedDtos == null || approvedDtos.isEmpty()) {
            return new SaveOutcome(0, 0);
        }

        merchantService.requireOwnedMerchants(userId, approvedDtos.stream()
                .map(d -> d.merchant() != null ? d.merchant().id() : null)
                .toList());
        Map<String, Long> merchantMap = resolveMerchantIds(userId, approvedDtos);
        Set<String> existingSignatures = findExistingSignatures(accountId, approvedDtos.stream().map(TransactionDto::date).toList());
        DedupeResult result = deduplicateTransactions(approvedDtos, existingSignatures, account, merchantMap);
        persistIfAny(account, result, fileName, fileHash);

        return new SaveOutcome(result.uniqueTransactions().size(), result.duplicateCount());
    }

    /**
     * (PF-809: extracted from {@link #saveTransactions}).
     */
    private Account validateAccountOwnership(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        if (!account.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied to account");
        }
        return account;
    }

    /**
     * Rejects the whole batch outright if its file hash matches a previously imported file
     * (PF-809: extracted from {@link #saveTransactions}).
     */
    private void checkForDuplicateFileImport(Long accountId, String fileHash) {
        if (fileHash != null && fileImportHistoryRepository.findByAccountIdAndFileHash(accountId, fileHash).isPresent()) {
            log.warn("Duplicate file hash detected during save. Account ID: {}, Hash: {}", accountId, fileHash);
            throw new DuplicateImportException("This file has already been imported.");
        }
    }

    /**
     * (PF-809: extracted from {@link #saveTransactions}).
     */
    private Map<String, Long> resolveMerchantIds(Long userId, List<TransactionDto> approvedDtos) {
        List<String> incomingDescriptions = approvedDtos.stream().map(TransactionDto::description).toList();
        return merchantService.findMatchingMerchantIds(userId, incomingDescriptions);
    }

    /**
     * The (date, amount, description, type) signatures of every already-saved transaction within
     * the date range spanned by the given dates (PF-809: extracted from {@link #saveTransactions};
     * shared with {@link #previewTransactions} in PF-898), to check incoming rows against.
     */
    private Set<String> findExistingSignatures(Long accountId, List<OffsetDateTime> dates) {
        OffsetDateTime earliest = dates.stream().min(OffsetDateTime::compareTo).orElse(OffsetDateTime.now());
        OffsetDateTime latest = dates.stream().max(OffsetDateTime::compareTo).orElse(OffsetDateTime.now());

        List<Transaction> existingTransactions = transactionRepository.findExistingForDuplicateCheck(accountId, earliest, latest);

        return existingTransactions.stream()
                .map(t -> buildSignature(t.getTransactionDate(), t.getAmount(), t.getDescription(), t.getType()))
                .collect(Collectors.toSet());
    }

    /**
     * The single definition of a transaction's duplicate-detection signature, used by both
     * preview and save so the two can never disagree about what counts as a duplicate.
     * (PF-898: extracted from the save path.)
     */
    private static String buildSignature(OffsetDateTime date, BigDecimal amount, String description, TransactionType type) {
        return String.format("%s_%s_%s_%s", date, amount.stripTrailingZeros(), description, type == null ? null : type.name());
    }

    /**
     * The transactions actually ready to insert, plus how many were skipped as duplicates of
     * either an existing row or an earlier row in this same batch (PF-809: extracted from
     * {@link #saveTransactions}).
     */
    private record DedupeResult(List<TransactionCreateRequest> uniqueTransactions, int duplicateCount) {
    }

    private DedupeResult deduplicateTransactions(List<TransactionDto> approvedDtos, Set<String> existingSignatures, Account account, Map<String, Long> merchantMap) {
        List<TransactionCreateRequest> uniqueTransactions = new ArrayList<>();
        Set<String> batchSignatures = new HashSet<>();
        int duplicateCount = 0;

        for (TransactionDto dto : approvedDtos) {
            String signature = buildSignature(dto.date(), dto.amount(), dto.description(), dto.type());

            boolean existsInDb = existingSignatures.contains(signature);
            boolean existsInBatch = batchSignatures.contains(signature);

            if (!existsInDb && !existsInBatch) {
                // a row's own reviewed merchant (PF-898, ownership already verified) wins over the
                // description match. a description with no existing link (PF-845) just leaves
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
     * Inserts the batch's unique transactions, updates the account balance, and records file
     * import history, only if there's anything to save (PF-809: extracted from
     * {@link #saveTransactions}).
     */
    private void persistIfAny(Account account, DedupeResult result, String fileName, String fileHash) {
        List<TransactionCreateRequest> uniqueTransactions = result.uniqueTransactions();
        if (!uniqueTransactions.isEmpty()) {
            transactionRepository.insertAll(uniqueTransactions);
            updateAccountBalance(account, uniqueTransactions);

            if (fileName != null && fileHash != null) {
                FileImportHistory history = FileImportHistory.builder()
                        .account(account)
                        .fileName(fileName)
                        .fileHash(fileHash)
                        .transactionCount(uniqueTransactions.size())
                        .build();
                fileImportHistoryRepository.save(history);
            }
            log.info("Successfully saved {} new transactions. Skipped {} duplicates.", uniqueTransactions.size(), result.duplicateCount());
        } else {
            log.info("No new transactions to save. All {} inputs were duplicates.", result.duplicateCount());
        }
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

    /**
     * Applies a batch of newly imported transactions to an account's running balance in one
     * optimistic-locked update.
     *
     * @param account         the account to update
     * @param newTransactions the transactions to apply
     * @throws org.springframework.dao.OptimisticLockingFailureException if the account was
     *                                                                    concurrently modified
     */
    private void updateAccountBalance(Account account, List<TransactionCreateRequest> newTransactions) {
        BigDecimal oldBalance = account.getCurrentBalance();

        Account updatedAccount = accountBalanceUpdateService.applyWithRetry(account.getUserId(), account, acc -> {
            Account result = acc;
            for (TransactionCreateRequest t : newTransactions) {
                result = result.applyTransaction(t);
            }
            return result;
        });

        log.info("Updated Account ID: {} balance. Old: {}, New: {}", updatedAccount.getId(), oldBalance, updatedAccount.getCurrentBalance());
    }
}