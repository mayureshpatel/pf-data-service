package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import com.mayureshpatel.pfdataservice.exception.DuplicateImportException;
import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.CategoryRule;
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
import java.util.stream.Stream;

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

        TransactionParser parser = parserFactory.getTransactionParser(bankName);
        List<CategoryRule> userRules = categorizer.loadRulesForUser(userId);

        try (Stream<Transaction> rawTransactionStream = parser.parse(accountId, fileContent)) {
            List<TransactionPreview> previews = rawTransactionStream
                    .map(t -> TransactionPreview.builder()
                            .date(t.getDate())
                            .description(t.getDescription())
                            .amount(t.getAmount())
                            .type(t.getType())
                            .suggestedCategory(categorizer.guessCategory(t, userRules))
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
        transaction.setDescription(dto.description());
        transaction.setAmount(dto.amount());
        transaction.setType(dto.type());
        return transaction;
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
}