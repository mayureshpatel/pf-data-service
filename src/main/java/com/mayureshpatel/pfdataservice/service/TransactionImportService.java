package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionPreviewDto;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import com.mayureshpatel.pfdataservice.exception.DuplicateImportException;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Autowired
    public TransactionImportService(TransactionRepository transactionRepository,
                                    AccountRepository accountRepository,
                                    CategoryRepository categoryRepository,
                                    FileImportHistoryRepository fileImportHistoryRepository,
                                    TransactionParserFactory parserFactory,
                                    TransactionCategorizer categorizer,
                                    CategoryRuleRepository categoryRuleRepository,
                                    MerchantService merchantService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.categoryRuleRepository = categoryRuleRepository;
        this.fileImportHistoryRepository = fileImportHistoryRepository;
        this.parserFactory = parserFactory;
        this.categorizer = categorizer;
        this.merchantService = merchantService;
    }

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
            List<TransactionPreviewDto> previews = rawTransactionStream
                    .map(t -> {
                        Long categoryId = categorizer.guessCategory(t, userRules, userCategories);
                        Category suggestedCategory = (categoryId != null && categoryId > 0)
                                ? userCategories.stream()
                                .filter(c -> c.getId().equals(categoryId))
                                .findFirst()
                                .orElse(null)
                                : null;
                        return TransactionPreviewDto.builder()
                                .date(t.getTransactionDate())
                                .postDate(t.getPostDate())
                                .description(t.getDescription())
                                .amount(t.getAmount())
                                .type(t.getType())
                                .suggestedCategory(CategoryDtoMapper.toDto(suggestedCategory))
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

    @Transactional
    public int saveTransactions(Long userId, Long accountId, List<TransactionDto> approvedDtos, String fileName, String fileHash) {
        log.info("Saving {} transactions for User: {}, Account ID: {}", approvedDtos.size(), userId, accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        if (!account.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied to account");
        }

        if (fileHash != null && fileImportHistoryRepository.findByAccountIdAndFileHash(accountId, fileHash).isPresent()) {
            log.warn("Duplicate file hash detected during save. Account ID: {}, Hash: {}", accountId, fileHash);
            throw new DuplicateImportException("This file has already been imported.");
        }

        if (approvedDtos == null || approvedDtos.isEmpty()) {
            return 0;
        }

        OffsetDateTime earliest = approvedDtos.stream().map(TransactionDto::date).min(OffsetDateTime::compareTo).orElse(OffsetDateTime.now());
        OffsetDateTime latest = approvedDtos.stream().map(TransactionDto::date).max(OffsetDateTime::compareTo).orElse(OffsetDateTime.now());

        List<String> incomingDescriptions = approvedDtos.stream().map(TransactionDto::description).toList();
        Map<String, Long> merchantMap = merchantService.findOrCreateMerchants(userId, incomingDescriptions);

        List<Transaction> existingTransactions = transactionRepository.findExistingForDuplicateCheck(accountId, earliest, latest);

        Set<String> existingSignatures = existingTransactions.stream()
            .map(t -> String.format("%s_%s_%s_%s", t.getTransactionDate(), t.getAmount().stripTrailingZeros(), t.getDescription(), t.getType().name()))
            .collect(Collectors.toSet());

        List<TransactionCreateRequest> uniqueTransactions = new ArrayList<>();
        Set<String> batchSignatures = new java.util.HashSet<>();
        int duplicateCount = 0;

        for (TransactionDto dto : approvedDtos) {
            String signature = String.format("%s_%s_%s_%s", dto.date(), dto.amount().stripTrailingZeros(), dto.description(), dto.type().name());

            boolean existsInDb = existingSignatures.contains(signature);
            boolean existsInBatch = batchSignatures.contains(signature);

            if (!existsInDb && !existsInBatch) {
                Long merchantId = merchantMap.get(dto.description());
                if (merchantId == null) {
                     // Fallback for edge cases
                     merchantId = merchantService.findOrCreateMerchant(userId, dto.description());
                     merchantMap.put(dto.description(), merchantId);
                }
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
            log.info("Successfully saved {} new transactions. Skipped {} duplicates.", uniqueTransactions.size(), duplicateCount);
        } else {
            log.info("No new transactions to save. All {} inputs were duplicates.", duplicateCount);
        }

        return uniqueTransactions.size();
    }

    private TransactionCreateRequest mapToEntity(TransactionDto dto) {
        return TransactionCreateRequest.builder()
                .transactionDate(dto.date())
                .postDate(dto.postDate())
                .description(dto.description())
                .amount(dto.amount())
                .type(dto.type().name())
                .build();
    }

    private void updateAccountBalance(Account account, List<TransactionCreateRequest> newTransactions) {
        BigDecimal oldBalance = account.getCurrentBalance();

        Account updatedAccount = account;
        for (TransactionCreateRequest t : newTransactions) {
            updatedAccount = updatedAccount.applyTransaction(t);
        }

        int updatedRows = accountRepository.updateBalance(updatedAccount.getUserId(), updatedAccount.getId(), updatedAccount.getCurrentBalance(), account.getVersion());
        if (updatedRows == 0) {
            throw new org.springframework.dao.OptimisticLockingFailureException("Account balance update failed due to concurrent modification");
        }

        log.info("Updated Account ID: {} balance. Old: {}, New: {}", updatedAccount.getId(), oldBalance, updatedAccount.getCurrentBalance());
    }
}