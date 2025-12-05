package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
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
    public List<TransactionPreview> previewTransactions(Long accountId, String bankName, byte[] fileContent, String fileName) {
        String fileHash = calculateFileHash(fileContent);
        if (fileImportHistoryRepository.existsByAccountIdAndFileHash(accountId, fileHash)) {
            throw new IllegalArgumentException("This file has already been imported for this account.");
        }

        TransactionParser parser = parserFactory.getTransactionParser(bankName);

        try (InputStream inputStream = new ByteArrayInputStream(fileContent)) {
            List<Transaction> rawTransactions = parser.parse(accountId, inputStream);

            return rawTransactions.stream()
                    .map(t -> TransactionPreview.builder()
                            .date(t.getDate())
                            .description(t.getDescription())
                            .amount(t.getAmount())
                            .type(t.getType())
                            .suggestedCategory(categorizer.guessCategory(t))
                            .build())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Error processing transaction file", e);
        }
    }

    @Transactional
    public int saveTransactions(Long accountId, List<Transaction> approvedTransactions, String fileName, String fileHash) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found with ID: " + accountId));

        if (fileHash != null && fileImportHistoryRepository.existsByAccountIdAndFileHash(accountId, fileHash)) {
            throw new IllegalArgumentException("This file has already been imported.");
        }

        List<Transaction> uniqueTransactions = new ArrayList<>();

        for (Transaction t : approvedTransactions) {
            boolean exists = transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
                    accountId, t.getDate(), t.getAmount(), t.getDescription(), t.getType()
            );

            if (!exists) {
                t.setAccount(account);
                uniqueTransactions.add(t);
            }
        }

        if (!uniqueTransactions.isEmpty()) {
            transactionRepository.saveAll(uniqueTransactions);
            updateAccountBalance(account, uniqueTransactions);

            // Record successful import
            if (fileName != null && fileHash != null) {
                FileImportHistory history = new FileImportHistory();
                history.setAccountId(accountId);
                history.setFileName(fileName);
                history.setFileHash(fileHash);
                history.setTransactionCount(uniqueTransactions.size());
                fileImportHistoryRepository.save(history);
            }
        }

        return uniqueTransactions.size();
    }

    private void updateAccountBalance(Account account, List<Transaction> newTransactions) {
        BigDecimal netChange = newTransactions.stream()
                .map(t -> t.getType() == TransactionType.INCOME ? t.getAmount() : t.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        account.setCurrentBalance(account.getCurrentBalance().add(netChange));
        accountRepository.save(account);
    }

    public String calculateFileHash(byte[] content) {
        return DigestUtils.md5DigestAsHex(content); // Or sha256Hex
    }
}
