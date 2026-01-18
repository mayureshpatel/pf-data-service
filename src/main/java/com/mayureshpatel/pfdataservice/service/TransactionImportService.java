package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import com.mayureshpatel.pfdataservice.exception.DuplicateImportException;
import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.FileImportHistory;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.FileImportHistoryRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParser;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParserFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TransactionImportService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final FileImportHistoryRepository fileImportHistoryRepository;
    private final TransactionParserFactory parserFactory;
    private final TransactionCategorizer categorizer;

    @Autowired
    public TransactionImportService(TransactionRepository transactionRepository,
                                    AccountRepository accountRepository,
                                    FileImportHistoryRepository fileImportHistoryRepository,
                                    TransactionParserFactory parserFactory,
                                    TransactionCategorizer categorizer) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.fileImportHistoryRepository = fileImportHistoryRepository;
        this.parserFactory = parserFactory;
        this.categorizer = categorizer;
    }

    @Transactional(readOnly = true)
    public List<TransactionPreview> previewTransactions(Long userId, Long accountId, String bankName, InputStream fileContent, String fileName) {
        log.info("Starting transaction preview for User: {}, Account ID: {}, Bank: {}, File: {}", userId, accountId, bankName, fileName);

        verifyAccountOwnership(userId, accountId);

        // Warning: Hashing an InputStream consumes it. We can't easily hash and then parse without buffering.
        // For strict streaming, we might skip hashing or calculate it while reading (which is complex).
        // For now, we will assume the caller might want to skip hashing for large streams, or we read it fully if hashing is mandatory.
        // Given the requirement is "Optimize File Upload" to use InputStream, let's skip the hash check for preview to save memory,
        // OR only hash if we buffer it. But the goal is to NOT buffer.
        // Let's defer hash check to "save" or accept that preview might not check for duplicates via hash.
        
        // However, the existing logic checks for duplicates.
        // To support true streaming without buffering the whole file in memory, we cannot calculate MD5 beforehand.
        // Compromise: Skip hash check for preview, or implement a "Mark/Reset" approach if the stream supports it, 
        // but MultipartFile input stream usually doesn't efficiently support mark/reset for large files.
        
        // Let's proceed with parsing directly. The hash check is more critical at "save" time.
        // If we strictly need hash check, we'd need to calculate it on the fly, but that doesn't help us "stop early".
        
        TransactionParser parser = parserFactory.getTransactionParser(bankName);

        try {
            List<Transaction> rawTransactions = parser.parse(accountId, fileContent);
            log.debug("Parsed {} raw transactions from file", rawTransactions.size());

            List<TransactionPreview> previews = rawTransactions.stream()
                    .map(t -> TransactionPreview.builder()
                            .date(t.getDate())
                            .description(t.getDescription())
                            .amount(t.getAmount())
                            .type(t.getType())
                            .suggestedCategory(categorizer.guessCategory(t))
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
    public int saveTransactions(Long userId, Long accountId, List<Transaction> approvedTransactions, String fileName, String fileHash) {
        log.info("Saving {} transactions for User: {}, Account ID: {}", approvedTransactions.size(), userId, accountId);

        Account account = verifyAccountOwnership(userId, accountId);

        if (fileHash != null && fileImportHistoryRepository.existsByAccountIdAndFileHash(accountId, fileHash)) {
            log.warn("Duplicate file hash detected during save. Account ID: {}, Hash: {}", accountId, fileHash);
            throw new DuplicateImportException("This file has already been imported.");
        }

        List<Transaction> uniqueTransactions = new ArrayList<>();
        int duplicateCount = 0;

        for (Transaction t : approvedTransactions) {
            boolean exists = transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
                    accountId, t.getDate(), t.getAmount(), t.getDescription(), t.getType()
            );

            if (!exists) {
                t.setAccount(account);
                uniqueTransactions.add(t);
            } else {
                duplicateCount++;
                if (log.isTraceEnabled()) {
                    log.trace("Skipping duplicate transaction: {} - {} - {}", t.getDate(), t.getDescription(), t.getAmount());
                }
            }
        }

        if (!uniqueTransactions.isEmpty()) {
            transactionRepository.saveAll(uniqueTransactions);
            updateAccountBalance(account, uniqueTransactions);

            if (fileName != null && fileHash != null) {
                FileImportHistory history = new FileImportHistory();
                history.setAccountId(accountId);
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

    private void updateAccountBalance(Account account, List<Transaction> newTransactions) {
        BigDecimal netChange = newTransactions.stream()
                .map(t -> t.getType() == TransactionType.INCOME ? t.getAmount() : t.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal oldBalance = account.getCurrentBalance();
        BigDecimal newBalance = oldBalance.add(netChange);

        account.setCurrentBalance(newBalance);
        accountRepository.save(account);

        log.info("Updated Account ID: {} balance. Old: {}, Net Change: {}, New: {}", account.getId(), oldBalance, netChange, newBalance);
    }

    public String calculateFileHash(InputStream inputStream) throws IOException {
        return DigestUtils.md5DigestAsHex(inputStream);
    }
    
    // Kept for backward compatibility if needed, but intended to be removed or unused for streaming
    public String calculateFileHash(byte[] content) {
        return DigestUtils.md5DigestAsHex(content);
    }

    private Account verifyAccountOwnership(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.error("Account not found. ID: {}", accountId);
                    return new EntityNotFoundException("Account not found with ID: " + accountId);
                });

        if (!account.getUser().getId().equals(userId)) {
            log.warn("Unauthorized access attempt. User: {}, Account: {}", userId, accountId);
            throw new AccessDeniedException("You do not have permission to access this account.");
        }
        return account;
    }
}