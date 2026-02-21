package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class CapitalOneCsvParser implements TransactionParser {
    private static final String HEADER_DATE = "Transaction Date";
    private static final String HEADER_DESCRIPTION = "Description";
    private static final String HEADER_DEBIT = "Debit";
    private static final String HEADER_CREDIT = "Credit";

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
        return BankName.CAPITAL_ONE;
    }

    @Override
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        try {
            CSVParser csvParser = CSV_FORMAT.parse(reader);
            return csvParser.stream()
                    .filter(csvRecord -> isValidRecord(csvRecord, HEADER_DATE))
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
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Failed to parse Capital One CSV", e);
        }
    }

    /**
     * Parses a CSV record into a {@link Transaction}.
     *
     * @param csvRecord the CSV record to parse
     * @return the parsed Transaction or empty if invalid
     */
    private Optional<Transaction> parseTransaction(CSVRecord csvRecord) {
        try {
            Transaction transaction = new Transaction();

            transaction.setTransactionDate(parseDate(csvRecord.get(HEADER_DATE), DATE_FORMATTER));
            transaction.setDescription(csvRecord.get(HEADER_DESCRIPTION));
            BigDecimal netAmount = calculateNetAmount(csvRecord);
            configureTransactionTypeAndAmount(transaction, netAmount);
            transaction.setCategory(null);

            return Optional.of(transaction);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Calculates the net amount for a transaction based on credit and debit amounts.
     *
     * @param csvRecord the CSV record containing the credit and debit amounts
     * @return the net amount
     */
    private BigDecimal calculateNetAmount(CSVRecord csvRecord) {
        BigDecimal credit = parseAmount(csvRecord, HEADER_CREDIT);
        BigDecimal debit = parseAmount(csvRecord, HEADER_DEBIT);
        return credit.subtract(debit);
    }
}