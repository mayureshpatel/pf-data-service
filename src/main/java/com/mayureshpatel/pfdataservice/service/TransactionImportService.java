package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParser;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParserFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionImportService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionParserFactory parserFactory;
    private final TransactionCategorizer categorizer;

    @Autowired
    public TransactionImportService(TransactionRepository transactionRepository,
                                    AccountRepository accountRepository,
                                    TransactionParserFactory parserFactory,
                                    TransactionCategorizer categorizer) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.parserFactory = parserFactory;
        this.categorizer = categorizer;
    }

    @Transactional(readOnly = true)
    public List<TransactionPreview> previewTransactions(Long accountId, String bankName, InputStream inputStream) {
        TransactionParser parser = parserFactory.getTransactionParser(bankName);

        List<Transaction> rawTransactions = parser.parse(accountId, inputStream);

        return rawTransactions.stream()
                .map(t -> TransactionPreview.builder()
                        .date(t.getDate())
                        .description(t.getDescription())
                        .amount(t.getAmount())
                        .type(t.getType())
                        .suggestedCategory(categorizer.guessCategory(t))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public int saveTransactions(Long accountId, List<Transaction> approvedTransactions) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found with ID: " + accountId));

        for (Transaction t : approvedTransactions) {
            t.setAccount(account);
            // In a real app, you would look up the Category Entity by name here
            // t.setCategory(categoryRepository.findByName(...));
        }

        if (!approvedTransactions.isEmpty()) {
            transactionRepository.saveAll(approvedTransactions);
            updateAccountBalance(account, approvedTransactions);
        }

        return approvedTransactions.size();
    }

    private void updateAccountBalance(Account account, List<Transaction> newTransactions) {
        BigDecimal netChange = newTransactions.stream()
                .map(t -> t.getType() == TransactionType.INCOME ? t.getAmount() : t.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        account.setCurrentBalance(account.getCurrentBalance().add(netChange));
        accountRepository.save(account);
    }
}
