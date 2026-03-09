package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import lombok.extern.slf4j.Slf4j;
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
import java.time.temporal.ChronoField;
import java.util.stream.Stream;

@Component
@Slf4j
public class SynovusCsvParser implements TransactionParser {
    private static final String HEADER_DATE = "Date";
    private static final String HEADER_DESCRIPTION = "Description";
    private static final String HEADER_CREDIT = "Credit";
    private static final String HEADER_DEBIT = "Debit";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[M/d/yyyy][MM/dd/yyyy][MMdd/yyyy][M/d/yy][MM/dd/yy]")
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
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
        return BankName.SYNOVUS;
    }

    @Override
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        if (inputStream == null) {
            throw new NullPointerException("InputStream cannot be null");
        }
        try {
            // Read until we find the header row starting with "Date"
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            StringBuilder csvContent = new StringBuilder();
            boolean headerFound = false;
            boolean isTabSeparated = false;

            while ((line = lineReader.readLine()) != null) {
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                String trimmedLine = line.trim();
                if (!headerFound) {
                    // Check if line starts with Date (handling potential quotes or BOM)
                    // We remove leading quotes to check for "Date"
                    String headerCheck = trimmedLine.replace("\"", "");
                    if (headerCheck.toLowerCase().startsWith("date")) {
                        headerFound = true;
                        isTabSeparated = line.contains("\t");
                        csvContent.append(line).append("\n");
                    }
                    continue;
                }
                // Skip the totals footer
                if (trimmedLine.contains("Totals:")) {
                    continue;
                }
                csvContent.append(line).append("\n");
            }

            if (!headerFound) {
                throw new CsvParsingException("Could not find header row starting with 'Date'");
            }

            CSVFormat format = isTabSeparated ? CSVFormat.TDF.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .setIgnoreSurroundingSpaces(true)
                    .get() : CSV_FORMAT;

            CSVParser csvParser = format.parse(new java.io.StringReader(csvContent.toString()));
            java.util.List<Transaction> transactions = new java.util.ArrayList<>();
            java.util.List<String> errors = new java.util.ArrayList<>();

            for (CSVRecord record : csvParser) {
                if (isValidRecord(record, HEADER_DATE)) {
                    try {
                        transactions.add(parseTransaction(record));
                    } catch (Exception e) {
                        errors.add("Row " + record.getRecordNumber() + ": " + e.getMessage());
                    }
                }
            }

            csvParser.close();

            if (!errors.isEmpty()) {
                throw new com.mayureshpatel.pfdataservice.exception.CsvParsingException("Failed to parse CSV with errors: " + String.join(", ", errors));
            }

            return transactions.stream();
        } catch (com.mayureshpatel.pfdataservice.exception.CsvParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Synovus CSV", e);
        }
    }

    /**
     * Parses a transaction from a CSV record.
     *
     * @param csvRecord the CSV record containing transaction data
     * @return the parsed transaction
     */
    private Transaction parseTransaction(CSVRecord csvRecord) {
        Transaction transaction = Transaction.builder()
                .transactionDate(parseDate(csvRecord.get(HEADER_DATE), DATE_TIME_FORMATTER))
                .description(csvRecord.get(HEADER_DESCRIPTION))
                .merchant(com.mayureshpatel.pfdataservice.domain.merchant.Merchant.builder()
                        .originalName(csvRecord.get(HEADER_DESCRIPTION))
                        .build())
                .build();

        BigDecimal netAmount = calculateNetAmount(csvRecord);
        transaction = configureTransactionTypeAndAmount(transaction, netAmount);

        return transaction;
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
        return credit.add(debit);
    }
}
