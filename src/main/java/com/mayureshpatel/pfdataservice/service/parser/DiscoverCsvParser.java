package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.model.BankName;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
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
public class DiscoverCsvParser implements TransactionParser {
    private static final String HEADER_DATE = "Trans. Date";
    private static final String HEADER_DESC = "Description";
    private static final String HEADER_AMOUNT = "Amount";
    private static final String HEADER_CATEGORY = "Category";
    private static final BankName BANK_NAME = BankName.DISCOVER;

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[M/d/yyyy][MM/dd/yyyy][yyyy-MM-dd]")
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
            throw new RuntimeException("Failed to parse Discover CSV", e);
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
            BigDecimal rawAmount = parseAmount(csvRecord);
            configureTransactionTypeAndAmount(transaction, rawAmount);
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
        String description = csvRecord.get(HEADER_DESC);
        String bankCategory = csvRecord.isMapped(HEADER_CATEGORY) ? csvRecord.get(HEADER_CATEGORY) : null;
        if (StringUtils.hasText(bankCategory)) {
            return description + " (" + bankCategory + ")";
        }
        return description;
    }

    private BigDecimal parseAmount(CSVRecord csvRecord) {
        if (!csvRecord.isMapped(HEADER_AMOUNT)) {
            return BigDecimal.ZERO;
        }
        String val = csvRecord.get(HEADER_AMOUNT);
        if (!StringUtils.hasText(val)) {
            return BigDecimal.ZERO;
        }
        String cleanVal = val.replaceAll("[^0-9.-]", "");
        try {
            return new BigDecimal(cleanVal);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void configureTransactionTypeAndAmount(Transaction transaction, BigDecimal rawAmount) {
        if (rawAmount.compareTo(BigDecimal.ZERO) < 0) {
            transaction.setType(TransactionType.INCOME);
            transaction.setAmount(rawAmount.abs());
        } else {
            transaction.setType(TransactionType.EXPENSE);
            transaction.setAmount(rawAmount);
        }
    }
}