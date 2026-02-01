package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import com.mayureshpatel.pfdataservice.exception.DuplicateImportException;
import com.mayureshpatel.pfdataservice.model.*;
import com.mayureshpatel.pfdataservice.repository.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.FileImportHistoryRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.categorization.VendorCleaner;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParser;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParserFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
    private final VendorCleaner vendorCleaner;

    @Autowired
    public TransactionImportService(TransactionRepository transactionRepository,
                                    AccountRepository accountRepository,
                                    CategoryRepository categoryRepository,
                                    FileImportHistoryRepository fileImportHistoryRepository,
                                    TransactionParserFactory parserFactory,
                                    TransactionCategorizer categorizer,
                                    VendorCleaner vendorCleaner) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.fileImportHistoryRepository = fileImportHistoryRepository;
        this.parserFactory = parserFactory;
        this.categorizer = categorizer;
        this.vendorCleaner = vendorCleaner;
    }

    @Transactional(readOnly = true)
    public List<TransactionPreview> previewTransactions(Long userId, Long accountId, String bankName, InputStream fileContent, String fileName) {
        log.info("Starting transaction preview for User: {}, Account ID: {}, Bank: {}, File: {}", userId, accountId, bankName, fileName);

        TransactionParser parser = parserFactory.getTransactionParser(bankName);
        List<CategoryRule> userRules = categorizer.loadRulesForUser(userId);
        List<VendorRule> vendorRules = vendorCleaner.loadRulesForUser(userId);
        List<Category> userCategories = categoryRepository.findByUserId(userId);

        try (Stream<Transaction> rawTransactionStream = parser.parse(accountId, fileContent)) {
            List<TransactionPreview> previews = rawTransactionStream
                    .map(t -> TransactionPreview.builder()
                            .date(t.getDate())
                            .postDate(t.getPostDate())
                            .description(t.getDescription())
                            .amount(t.getAmount())
                            .type(t.getType())
                            .suggestedCategory(categorizer.guessCategory(t, userRules, userCategories))
                            .vendorName(vendorCleaner.cleanVendorName(t.getDescription(), vendorRules))
                            .build())
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
                .orElseThrow(() -> new EntityNotFoundException("Account not found with ID: " + accountId));

        if (fileHash != null && fileImportHistoryRepository.existsByAccountIdAndFileHash(accountId, fileHash)) {
            log.warn("Duplicate file hash detected during save. Account ID: {}, Hash: {}", accountId, fileHash);
            throw new DuplicateImportException("This file has already been imported.");
        }

        List<Transaction> uniqueTransactions = new ArrayList<>();
        int duplicateCount = 0;

        for (TransactionDto dto : approvedDtos) {
            boolean exists = transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
                    accountId, dto.date(), dto.amount(), dto.description(), dto.type()
            );

            if (!exists) {
                Transaction t = mapToEntity(dto);
                t.setAccount(account);
                uniqueTransactions.add(t);
            } else {
                duplicateCount++;
                if (log.isTraceEnabled()) {
                    log.trace("Skipping duplicate transaction: {} - {} - {}", dto.date(), dto.description(), dto.amount());
                }
            }
        }

        if (!uniqueTransactions.isEmpty()) {
            transactionRepository.saveAll(uniqueTransactions);
            updateAccountBalance(account, uniqueTransactions);

            if (fileName != null && fileHash != null) {
                FileImportHistory history = new FileImportHistory();
                history.setAccount(account);
                history.setFileName(fileName);
                history.setFileHash(fileHash);
                history.setTransactionCount(uniqueTransactions.size());
                fileImportHistoryRepository.save(history);
            }
            log.info("Successfully saved {} new transactions. Skipped {} duplicates.", uniqueTransactions.size(), duplicateCount);
        } else {
            log.info("No new transactions to save. All {} inputs were duplicates.", duplicateCount);
        }

        return uniqueTransactions.size();
    }

    private Transaction mapToEntity(TransactionDto dto) {
        Transaction transaction = new Transaction();
        transaction.setDate(dto.date());
        transaction.setPostDate(dto.postDate());
        transaction.setDescription(dto.description());
        // For imports, original vendor name is the description
        transaction.setOriginalVendorName(dto.description());
        transaction.setAmount(dto.amount());
        transaction.setType(dto.type());
        transaction.setVendorName(dto.vendorName());
        return transaction;
    }

    private void updateAccountBalance(Account account, List<Transaction> newTransactions) {
        BigDecimal oldBalance = account.getCurrentBalance();

        for (Transaction t : newTransactions) {
            account.applyTransaction(t);
        }

        accountRepository.save(account);

        log.info("Updated Account ID: {} balance. Old: {}, New: {}", account.getId(), oldBalance, account.getCurrentBalance());
    }

    public String calculateFileHash(InputStream inputStream) throws IOException {
        return DigestUtils.md5DigestAsHex(inputStream);
    }

    // Kept for backward compatibility if needed, but intended to be removed or unused for streaming
    public String calculateFileHash(byte[] content) {
        return DigestUtils.md5DigestAsHex(content);
    }
}