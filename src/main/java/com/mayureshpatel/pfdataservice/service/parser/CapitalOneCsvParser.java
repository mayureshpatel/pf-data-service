package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class CapitalOneCsvParser implements TransactionParser {
    private static final String HEADER_DATE = "Transaction Date";
    private static final String HEADER_DESCRIPTION = "Description";
    private static final String HEADER_DEBIT = "Debit";
    private static final String HEADER_CREDIT = "Credit";

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd][M/d/yyyy][MM/dd/yyyy]")
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
        return BankName.CAPITAL_ONE;
    }

    @Override
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        try {
            CSVParser csvParser = CSV_FORMAT.parse(reader);
            List<Transaction> transactions = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (CSVRecord record : csvParser) {
                if (isValidRecord(record, HEADER_DATE)) {
                    try {
                        transactions.add(parseTransaction(record));
                    } catch (Exception e) {
                        errors.add("Row " + record.getRecordNumber() + ": " + e.getMessage());
                    }
                }
            }

            try {
                csvParser.close();
                reader.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to close CSV parser resources", e);
            }

            if (!errors.isEmpty()) {
                throw new CsvParsingException("Failed to parse CSV with errors: " + String.join(", ", errors));
            }

            return transactions.stream();
        } catch (CsvParsingException e) {
            throw e;
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
     * @return the parsed Transaction
     */
    private Transaction parseTransaction(CSVRecord csvRecord) {
        Transaction transaction = Transaction.builder()
                .transactionDate(parseDate(csvRecord.get(HEADER_DATE), DATE_FORMATTER))
                .description(csvRecord.get(HEADER_DESCRIPTION))
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
        return credit.subtract(debit);
    }
}
