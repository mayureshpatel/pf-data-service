package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Shared reading rules for Fidelity's CSV exports: leading blank lines before the header, a header
 * row that is not necessarily line 1, {@code MM/dd/yyyy} dates and a legal disclaimer block after
 * the data. Package-private; the two Fidelity parsers are the only users.
 */
final class FidelityCsvSupport {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.ROOT);
    private static final Pattern DATE_PREFIX = Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}$");

    /** Byte order mark some exports start with. */
    private static final char BOM = (char) 0xFEFF;

    private FidelityCsvSupport() {
    }

    /**
     * Reads the data rows that follow the header row beginning with {@code headerPrefix}, mapping
     * each through {@code mapper} (which returns null to skip a row). Blank lines and the trailing
     * disclaimer lines (a single field that is not a date) are ignored.
     */
    static <T> List<T> readRows(InputStream inputStream, String headerPrefix, String label,
                                Function<CSVRecord, T> mapper) {
        try {
            String text = stripBom(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            int headerStart = findHeaderStart(text, headerPrefix);
            if (headerStart < 0) {
                if (text.isBlank()) {
                    return List.of();
                }
                throw new CsvParsingException("Fidelity " + label + " CSV has no header row starting with '"
                        + headerPrefix + "'.");
            }
            return parseRows(text.substring(headerStart), mapper);
        } catch (CsvParsingException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new CsvParsingException("Failed to parse Fidelity " + label + " CSV", e);
        }
    }

    /** Drops the leading byte order mark, if the export has one. */
    private static String stripBom(String text) {
        return !text.isEmpty() && text.charAt(0) == BOM ? text.substring(1) : text;
    }

    /** Parses the CSV text that starts at the header row, skipping disclaimers and null-mapped rows. */
    private static <T> List<T> parseRows(String csvText, Function<CSVRecord, T> mapper) throws IOException {
        try (CSVParser csvParser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get()
                .parse(new StringReader(csvText))) {
            List<T> rows = new ArrayList<>();
            for (CSVRecord csvRecord : csvParser) {
                if (isDisclaimer(csvRecord)) {
                    continue;
                }
                T row = mapper.apply(csvRecord);
                if (row != null) {
                    rows.add(row);
                }
            }
            return rows;
        }
    }

    /** Offset of the first line starting with the header prefix, or -1. */
    private static int findHeaderStart(String text, String headerPrefix) {
        int offset = 0;
        for (String line : text.split("\n", -1)) {
            if (line.stripLeading().startsWith(headerPrefix)) {
                return offset;
            }
            offset += line.length() + 1;
        }
        return -1;
    }

    private static boolean isDisclaimer(CSVRecord csvRecord) {
        return csvRecord.size() == 1 && !DATE_PREFIX.matcher(csvRecord.get(0)).matches();
    }

    /** Parses an {@code MM/dd/yyyy} date to UTC midnight. */
    static OffsetDateTime parseDate(String value, long recordNumber) {
        try {
            return LocalDate.parse(value.trim(), DATE_FORMAT).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        } catch (RuntimeException e) {
            throw new CsvParsingException("Invalid date '" + value + "' at row " + recordNumber
                    + "; expected MM/dd/yyyy.", e);
        }
    }

    /**
     * Requires a money-in amount to be strictly positive: a negative or zero (including blank) value
     * on a row that resolved to CONTRIBUTION or EARNINGS would otherwise create phantom new money.
     */
    static java.math.BigDecimal requirePositive(java.math.BigDecimal amount, String kind, long recordNumber) {
        if (amount.signum() <= 0) {
            throw new CsvParsingException("Fidelity row '" + kind + "' at row " + recordNumber
                    + " has a non-positive amount (" + amount.toPlainString() + "); expected a positive amount.");
        }
        return amount;
    }
}
