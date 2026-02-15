package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class CapitalOneCsvParser implements TransactionParser {
    private static final String HEADER_DATE = "Transaction Date";
    private static final String HEADER_DESCRIPTION = "Description";
    private static final String HEADER_CATEGORY = "Category";
    private static final String HEADER_DEBIT = "Debit";
    private static final String HEADER_CREDIT = "Credit";
    private static final BankName BANK_NAME = BankName.CAPITAL_ONE;

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd][M/d/yyyy][MM/dd/yyyy]")
            .toFormatter();

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .setIgnoreSurroundingSpaces(true)
            .get();

    @Override
    public BankName getBankName() {
        return BANK_NAME;
    }

    @Override
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            CSVParser csvParser = CSV_FORMAT.parse(reader);
            return csvParser.stream()
                    .filter(this::isValidRecord)
                    .map(this::parseTransaction)
                    .flatMap(Optional::stream)
                    .onClose(() -> {
                        try {
                            csvParser.close();
                            reader.close();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to close CSV parser resources", e);
                        }
                    });
        } catch (Exception e) {
            try {
                reader.close();
            } catch (Exception ignored) {}
            throw new RuntimeException("Failed to parse Capital One CSV", e);
        }
    }

    private boolean isValidRecord(CSVRecord csvRecord) {
        return csvRecord.isMapped(HEADER_DATE) && StringUtils.hasText(csvRecord.get(HEADER_DATE));
    }

    private Optional<Transaction> parseTransaction(CSVRecord csvRecord) {
        try {
            Transaction transaction = new Transaction();
            transaction.setDate(parseDate(csvRecord.get(HEADER_DATE)));
            transaction.setDescription(buildDescription(csvRecord));
            BigDecimal netAmount = calculateNetAmount(csvRecord);
            configureTransactionTypeAndAmount(transaction, netAmount);
            transaction.setCategory(null);
            return Optional.of(transaction);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    private String buildDescription(CSVRecord csvRecord) {
        String description = csvRecord.get(HEADER_DESCRIPTION);
        String bankCategory = csvRecord.isMapped(HEADER_CATEGORY) ? csvRecord.get(HEADER_CATEGORY) : null;
        if (StringUtils.hasText(bankCategory)) {
            return description + " (" + bankCategory + ")";
        }
        return description;
    }

    private BigDecimal calculateNetAmount(CSVRecord csvRecord) {
        BigDecimal credit = parseAmount(csvRecord, HEADER_CREDIT);
        BigDecimal debit = parseAmount(csvRecord, HEADER_DEBIT);
        return credit.subtract(debit);
    }

    private void configureTransactionTypeAndAmount(Transaction transaction, BigDecimal netAmount) {
        if (netAmount.compareTo(BigDecimal.ZERO) >= 0) {
            transaction.setType(TransactionType.INCOME);
            transaction.setAmount(netAmount);
        } else {
            transaction.setType(TransactionType.EXPENSE);
            transaction.setAmount(netAmount.abs());
        }
    }

    private BigDecimal parseAmount(CSVRecord csvRecord, String header) {
        if (!csvRecord.isMapped(header)) {
            return BigDecimal.ZERO;
        }
        String stringVal = csvRecord.get(header);
        if (!StringUtils.hasText(stringVal)) {
            return BigDecimal.ZERO;
        }
        String amount = stringVal.replaceAll("[^0-9.-]", "");
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}