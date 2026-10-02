package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Stream;

/**
 * Fallback parser for banks without a dedicated implementation. Rather than fixed header names,
 * it fuzzy-matches common header name variants (e.g. "Trans Date", "Transaction Date", "Date" all
 * resolve to the date column) via {@link #identifyColumns}, and supports either a single signed
 * amount column or separate debit/credit columns. A row that parses to a zero amount is stored as
 * a real {@code $0.00} transaction (PF-822) -- every other parser in this codebase
 * ({@link StandardCsvParser}, {@link CapitalOneCsvParser}, {@link DiscoverCsvParser},
 * {@link SynovusCsvParser}) already does this via {@link TransactionParser}'s own
 * {@code configureTransactionTypeAndAmount}/{@code configureCreditCardTransactionTypeAndAmount}
 * default methods, which have no zero-amount special case at all; this parser used to be the one
 * exception, silently skipping such rows as presumed pending/auth-hold artifacts.
 */
@Component
@Slf4j
public class UniversalCsvParser implements TransactionParser {

    // common date formats to try
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    /**
     * {@inheritDoc}
     */
    @Override
    public BankName getBankName() {
        return BankName.UNIVERSAL;
    }

    /**
     * {@inheritDoc}
     *
     * @throws com.mayureshpatel.pfdataservice.exception.CsvParsingException if any row fails to
     *                                                                       parse, or if required columns can't be identified
     */
    @Override
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            CSVParser parser = openHeaderParser(reader);
            UniversalCsvColumnDetector.ColumnMapping mapping = UniversalCsvColumnDetector.detect(parser.getHeaderMap());
            ParseOutcome outcome = parseRecords(parser, mapping);
            closeQuietlyLogging(parser, reader);

            if (!outcome.errors().isEmpty()) {
                throw new com.mayureshpatel.pfdataservice.exception.CsvParsingException("Failed to parse CSV with errors: " + String.join("; ", outcome.errors()));
            }

            return outcome.transactions().stream();

        } catch (com.mayureshpatel.pfdataservice.exception.CsvParsingException e) {
            throw e;
        } catch (Exception e) {
            try {
                reader.close();
            } catch (Exception closeException) {
                log.warn("Failed to close reader during exception handling", closeException);
            }
            throw new com.mayureshpatel.pfdataservice.exception.CsvParsingException("Failed to parse Universal CSV", e);
        }
    }

    /**
     * Just parses the header to find columns (PF-809: extracted from {@link #parse}).
     */
    private CSVParser openHeaderParser(BufferedReader reader) throws java.io.IOException {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .get()
                .parse(reader);
    }

    /**
     * A parsed file's successfully-mapped transactions alongside any per-row errors (PF-809:
     * extracted from {@link #parse}) -- collected together rather than thrown immediately, so a
     * malformed row doesn't prevent every other row's error from also being reported.
     */
    private record ParseOutcome(List<Transaction> transactions, List<String> errors) {
    }

    private ParseOutcome parseRecords(CSVParser parser, UniversalCsvColumnDetector.ColumnMapping mapping) {
        List<Transaction> transactions = new java.util.ArrayList<>();
        List<String> errors = new java.util.ArrayList<>();

        for (CSVRecord record : parser) {
            try {
                Transaction t = parseRecord(record, mapping);
                if (t != null) {
                    transactions.add(t);
                }
            } catch (Exception e) {
                errors.add("Row " + record.getRecordNumber() + ": " + e.getMessage());
            }
        }

        return new ParseOutcome(transactions, errors);
    }

    /**
     * Unlike this codebase's other CSV parsers, a close failure here is only logged, not raised as
     * its own parse failure (PF-809: extracted from {@link #parse}, behavior unchanged).
     */
    private void closeQuietlyLogging(CSVParser parser, BufferedReader reader) {
        try {
            parser.close();
            reader.close();
        } catch (Exception e) {
            log.error("Error closing CSV resources", e);
        }
    }

    /**
     * Parses a single CSV record into a Transaction object.
     *
     * @param record  CSV record to parse
     * @param mapping Column mapping configuration
     * @return Parsed Transaction object
     */
    private Transaction parseRecord(CSVRecord record, UniversalCsvColumnDetector.ColumnMapping mapping) {
        LocalDate localDate = parseDate(record.get(mapping.dateCol()));
        if (localDate == null) return null;
        OffsetDateTime date = localDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);

        OffsetDateTime postDate = resolvePostDate(record, mapping);
        String description = record.get(mapping.descCol());
        AmountResult amountResult = resolveAmountAndType(record, mapping);

        return Transaction.builder()
                .transactionDate(date)
                .postDate(postDate)
                .description(description)
                .amount(amountResult.amount())
                .type(amountResult.type())
                .build();
    }

    /**
     * The post date column, independent of the main transaction date (PF-809: extracted from
     * {@link #parseRecord}) -- {@code null} when there's no distinct post-date column, or its own
     * value doesn't parse.
     */
    private OffsetDateTime resolvePostDate(CSVRecord record, UniversalCsvColumnDetector.ColumnMapping mapping) {
        if (mapping.postDateCol() == null || mapping.postDateCol().equals(mapping.dateCol())) {
            return null;
        }
        LocalDate localPostDate = parseDate(record.get(mapping.postDateCol()));
        return localPostDate != null ? localPostDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC) : null;
    }

    private record AmountResult(BigDecimal amount, TransactionType type) {
    }

    /**
     * Resolves the signed amount and its implied {@link TransactionType} from whichever
     * amount-column strategy this file uses (PF-809: extracted from {@link #parseRecord}) --
     * a single signed amount column, or separate debit/credit columns, matching
     * {@link UniversalCsvColumnDetector}'s own validation that at least one of these is present.
     */
    private AmountResult resolveAmountAndType(CSVRecord record, UniversalCsvColumnDetector.ColumnMapping mapping) {
        if (mapping.debitCol() != null && mapping.creditCol() != null) {
            BigDecimal debit = parseAmount(record.get(mapping.debitCol()));
            BigDecimal credit = parseAmount(record.get(mapping.creditCol()));
            if (debit.compareTo(BigDecimal.ZERO) > 0) {
                return new AmountResult(debit, TransactionType.EXPENSE);
            } else if (credit.compareTo(BigDecimal.ZERO) > 0) {
                return new AmountResult(credit, TransactionType.INCOME);
            }
            return new AmountResult(BigDecimal.ZERO, TransactionType.EXPENSE);
        }
        if (mapping.amountCol() != null) {
            BigDecimal rawAmount = parseAmount(record.get(mapping.amountCol()));
            return rawAmount.compareTo(BigDecimal.ZERO) < 0
                    ? new AmountResult(rawAmount.abs(), TransactionType.EXPENSE)
                    : new AmountResult(rawAmount, TransactionType.INCOME);
        }
        if (mapping.debitCol() != null) {
            return new AmountResult(parseAmount(record.get(mapping.debitCol())), TransactionType.EXPENSE);
        }
        if (mapping.creditCol() != null) {
            return new AmountResult(parseAmount(record.get(mapping.creditCol())), TransactionType.INCOME);
        }
        return new AmountResult(BigDecimal.ZERO, TransactionType.EXPENSE);
    }

    /**
     * Parses a transaction amount string into a {@link BigDecimal} object.
     * <p>
     * package-private rather than private: the null-vs-blank branches below can't both be reached
     * through {@link #parse}, since a missing CSV field always arrives here as "" (never a literal
     * null), and {@code CSVFormat}'s own {@code setTrim(true)} strips real whitespace before this
     * method ever sees it. Direct unit tests are the only way to exercise both independently.
     *
     * @param amountStr the amount string to parse
     * @return the parsed BigDecimal amount or BigDecimal.ZERO if parsing fails
     */
    BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            return BigDecimal.ZERO;
        }

        // remove currency symbols, commas
        String clean = amountStr.replace("$", "").replace(",", "").trim();

        // handle parenthesis for negative: (100.00) -> -100.00
        if (clean.startsWith("(") && clean.endsWith(")")) {
            clean = "-" + clean.substring(1, clean.length() - 1);
        }

        try {
            return new BigDecimal(clean);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format detected: " + amountStr, e);
        }
    }

    /**
     * Parses a date string into a {@link LocalDate} object using multiple date formats.
     *
     * @param dateStr the date string to parse
     * @return the parsed LocalDate or null if parsing fails
     * @throws IllegalArgumentException if no valid date format is found
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }
        throw new IllegalArgumentException("Unknown date format: " + dateStr);
    }
}
