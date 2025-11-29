package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.CategoryTotal;
import com.mayureshpatel.pfdataservice.dto.DashboardData;
import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public DashboardData getDashboardData(Long userId, int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        BigDecimal income = this.transactionRepository.getSumByDateRange(userId, startDate, endDate, TransactionType.INCOME);
        BigDecimal expense = this.transactionRepository.getSumByDateRange(userId, startDate, endDate, TransactionType.EXPENSE);

        income = (income == null) ? BigDecimal.ZERO : income;
        expense = (expense == null) ? BigDecimal.ZERO : expense;

        List<CategoryTotal> breakdown = this.transactionRepository.findCategoryTotals(userId, startDate, endDate);

        return DashboardData.builder()
                .totalIncome(income)
                .totalExpense(expense)
                .netSavings(income.subtract(expense))
                .categoryBreakdown(breakdown)
                .build();
    }

    @Transactional
    public int importCsv(Long accountId, InputStream inputStream) throws IOException {
        Account account = this.accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));

        List<Transaction> transactionsToSave = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());

            for (CSVRecord csvRecord : csvParser) {
                String dateString = csvRecord.get("Date");
                String description = csvRecord.get("Description");
                String amountString = csvRecord.get("Amount");

                BigDecimal amount = new BigDecimal(amountString.replace("$", "").replace(",", ""));
                LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("MM/dd/yyy"));

                TransactionType type = amount.compareTo(BigDecimal.ZERO) < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;

                BigDecimal finalAmount = amount.abs();

                Transaction transaction = new Transaction();
                transaction.setAccount(account);
                transaction.setDate(date);
                transaction.setDescription(description);
                transaction.setAmount(finalAmount);
                transaction.setType(type);

                transactionsToSave.add(transaction);
            }
        }

        this.transactionRepository.saveAll(transactionsToSave);
        return transactionsToSave.size();
    }
}
