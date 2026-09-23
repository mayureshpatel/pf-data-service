package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

/**
 * Parser for the application's own generic CSV format: {@code description}, {@code amount}
 * (negative for expenses, positive for income), and {@code date} columns, case-insensitively
 * matched by exact header name (unlike {@link UniversalCsvParser}, which fuzzy-matches header
 * name variants).
 */
@Component
public class StandardCsvParser implements TransactionParser {
    /**
     * {@inheritDoc}
     */
    @Override
    public BankName getBankName() {
        return BankName.STANDARD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            CSVParser csvParser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .get()
                    .parse(reader);
            
            return csvParser.stream()
                    .map(this::mapToTransaction)
                    .onClose(() -> {
                        try {
                            csvParser.close();
                            reader.close();
                        } catch (Exception e) {
                            throw new CsvParsingException("Failed to close CSV parser resources", e);
                        }
                    });
        } catch (Exception e) {
            try {
                reader.close();
            } catch (Exception ignored) {
            }
            throw new CsvParsingException("Failed to parse Standard CSV", e);
        }
    }

    /**
     * Maps one CSV row to a transaction. The amount's sign determines the transaction type
     * (negative -> expense, non-negative -> income) and is normalized to an absolute value. A
     * zero amount is therefore stored as a real {@code $0.00} INCOME transaction, not skipped --
     * deliberately, matching every other parser in this codebase (PF-822).
     *
     * @param csvRecord the CSV record to map
     * @return the parsed transaction
     */
    private Transaction mapToTransaction(CSVRecord csvRecord) {
        Transaction t = Transaction.builder()
                .description(csvRecord.get("description"))
                .build();

        String amountStr = csvRecord.get("amount").replace("$", "").replace(",", "");
        BigDecimal amount = new BigDecimal(amountStr);

        TransactionType type = amount.compareTo(BigDecimal.ZERO) < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;

        return t.toBuilder()
                .amount(amount.abs())
                .transactionDate(parseDate(csvRecord.get("date")))
                .type(type)
                .build();
    }

    /**
     * Parses the date column, accepting either a full ISO-8601 offset-date-time (e.g.
     * "2025-01-15T00:00:00Z") or a plain ISO-8601 local date (e.g. "2025-01-15"), the latter
     * defaulted to UTC midnight -- never a hardcoded or system-inferred local timezone (PF-311).
     * A date-only value is the most likely real input for this format, which the UI advertises to
     * users simply as "Generic format (Date, Description, Amount)"; it previously threw an
     * unhandled {@link DateTimeParseException} instead of importing successfully.
     *
     * @param dateStr the raw date column value
     * @return the parsed date, at UTC midnight if no time/offset was present
     */
    private OffsetDateTime parseDate(String dateStr) {
        try {
            return OffsetDateTime.parse(dateStr);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        }
    }
}
