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
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
@Slf4j
public class UniversalCsvParser implements TransactionParser {

    private static final Pattern DATE_PATTERN = Pattern.compile("^(transaction\\s*date|trans\\s*date|date)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern POST_DATE_PATTERN = Pattern.compile("^(post\\s*date|posted\\s*date)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESC_PATTERN = Pattern.compile("^(description|original\\s*description|memo|payee|merchant)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^(amount|amount\\s*\\(?\\$\\)?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEBIT_PATTERN = Pattern.compile("^(debit|debit\\s*\\(?\\$\\)?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREDIT_PATTERN = Pattern.compile("^(credit|credit\\s*\\(?\\$\\)?)$", Pattern.CASE_INSENSITIVE);

    // Common date formats to try
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    @Override
    public BankName getBankName() {
        return BankName.UNIVERSAL;
    }

    @Override
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            // First, just parse the header to find columns
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .get()
                    .parse(reader);

            Map<String, Integer> headerMap = parser.getHeaderMap();
            ColumnMapping mapping = identifyColumns(headerMap);

            return parser.stream()
                    .map(record -> parseRecord(record, mapping))
                    .filter(Objects::nonNull) // Skip failed rows
                    .onClose(() -> {
                        try {
                            parser.close();
                            reader.close();
                        } catch (Exception e) {
                            log.error("Error closing CSV resources", e);
                        }
                    });

        } catch (Exception e) {
            try {
                reader.close();
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Failed to parse Universal CSV", e);
        }
    }

    /**
     * Identifies column mappings based on header names.
     *
     * @param headerMap Map of header names to column indices
     * @return ColumnMapping object with identified column names
     */
    private ColumnMapping identifyColumns(Map<String, Integer> headerMap) {
        String dateCol = null;
        String postDateCol = null;
        String descCol = null;
        String amountCol = null;
        String debitCol = null;
        String creditCol = null;

        for (String col : headerMap.keySet()) {
            String cleanCol = col.trim();
            if (dateCol == null && DATE_PATTERN.matcher(cleanCol).matches()) {
                dateCol = col;
            } else if (postDateCol == null && POST_DATE_PATTERN.matcher(cleanCol).matches()) {
                postDateCol = col;
            } else if (descCol == null && DESC_PATTERN.matcher(cleanCol).matches()) {
                descCol = col;
            } else if (amountCol == null && AMOUNT_PATTERN.matcher(cleanCol).matches()) {
                amountCol = col;
            } else if (debitCol == null && DEBIT_PATTERN.matcher(cleanCol).matches()) {
                debitCol = col;
            } else if (creditCol == null && CREDIT_PATTERN.matcher(cleanCol).matches()) {
                creditCol = col;
            }
        }

        // fallback: if "Date" not found but "Post Date" is, use Post Date as Date
        if (dateCol == null && postDateCol != null) {
            dateCol = postDateCol;
        }

        if (dateCol == null) {
            throw new IllegalArgumentException("Could not find a valid 'Date' column in CSV headers.");
        }
        if (descCol == null) {
            throw new IllegalArgumentException("Could not find a valid 'Description' column in CSV headers.");
        }
        if (amountCol == null && (debitCol == null || creditCol == null)) {
            // need either Amount OR (Debit AND Credit)
            if (debitCol == null && creditCol == null) {
                throw new IllegalArgumentException("Could not find valid 'Amount' or 'Debit/Credit' columns.");
            }
        }

        log.info("Universal Parser Mapped Columns - Date: {}, PostDate: {}, Desc: {}, Amount: {}, Debit: {}, Credit: {}",
                dateCol, postDateCol, descCol, amountCol, debitCol, creditCol);

        return new ColumnMapping(dateCol, postDateCol, descCol, amountCol, debitCol, creditCol);
    }

    /**
     * Parses a single CSV record into a Transaction object.
     *
     * @param record  CSV record to parse
     * @param mapping Column mapping configuration
     * @return Parsed Transaction object
     */
    private Transaction parseRecord(CSVRecord record, ColumnMapping mapping) {
        try {
            // parse date
            OffsetDateTime date = OffsetDateTime.from(parseDate(record.get(mapping.dateCol)));
            OffsetDateTime postDate = mapping.postDateCol != null && !mapping.postDateCol.equals(mapping.dateCol)
                    ? OffsetDateTime.from(parseDate(record.get(mapping.postDateCol)))
                    : null;

            // parse description
            String description = record.get(mapping.descCol);

            // parse amount and transaction type
            BigDecimal amount = BigDecimal.ZERO;
            TransactionType type = TransactionType.EXPENSE; // Default

            if (mapping.debitCol != null && mapping.creditCol != null) {
                // two column strategy
                String debitStr = record.get(mapping.debitCol);
                String creditStr = record.get(mapping.creditCol);

                BigDecimal debit = parseAmount(debitStr);
                BigDecimal credit = parseAmount(creditStr);

                if (debit.compareTo(BigDecimal.ZERO) > 0) {
                    amount = debit;
                } else if (credit.compareTo(BigDecimal.ZERO) > 0) {
                    amount = credit;
                    type = TransactionType.INCOME;
                }
            } else if (mapping.amountCol != null) {
                // single amount strategy
                BigDecimal rawAmount = parseAmount(record.get(mapping.amountCol));

                if (rawAmount.compareTo(BigDecimal.ZERO) < 0) {
                    amount = rawAmount.abs();
                } else {
                    type = TransactionType.INCOME;
                    amount = rawAmount;
                }
            } else if (mapping.debitCol != null) {
                amount = parseAmount(record.get(mapping.debitCol));
            } else if (mapping.creditCol != null) {
                amount = parseAmount(record.get(mapping.creditCol));
                type = TransactionType.INCOME;
            }

            // skip zero-amount transactions (often pending or auth holds)
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                return null;
            }

            Transaction t = new Transaction();
            t.setTransactionDate(date);
            t.setPostDate(postDate);
            t.setDescription(description);
            t.setAmount(amount);
            t.setType(type);
            t.getMerchant().setOriginalName(description);

            return t;

        } catch (Exception e) {
            log.warn("Skipping invalid row in CSV: {} - Error: {}", record, e.getMessage());
            return null;
        }
    }

    /**
     * Parses a transaction amount string into a {@link BigDecimal} object.
     *
     * @param amountStr the amount string to parse
     * @return the parsed BigDecimal amount or BigDecimal.ZERO if parsing fails
     */
    private BigDecimal parseAmount(String amountStr) {
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
            return BigDecimal.ZERO;
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

    private record ColumnMapping(
            String dateCol,
            String postDateCol,
            String descCol,
            String amountCol,
            String debitCol,
            String creditCol
    ) {
    }
}
