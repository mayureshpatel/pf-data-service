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
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Parser for Ally Bank savings account CSV exports: {@code Date, Time, Amount, Type, Description}
 * columns. The amount is always positive and the {@code Type} column carries the direction.
 * <p>
 * Mapping: a {@code Deposit} whose description is "Interest Paid" is {@code EARNINGS}; any other
 * {@code Deposit} whose description contains "Transfer" is {@code TRANSFER_IN}; any other
 * {@code Deposit} is {@code INCOME}; a {@code Withdrawal} is {@code TRANSFER_OUT}. The withdrawal
 * mapping is unverified against a real export (none was supplied).
 */
@Component
public class AllySavingsCsvParser implements TransactionParser {
    /**
     * {@inheritDoc}
     */
    @Override
    public BankName getBankName() {
        return BankName.ALLY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try (reader; CSVParser csvParser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get()
                .parse(reader)) {
            // materialized eagerly so both resources are closed before returning
            return csvParser.stream()
                    .filter(r -> isValidRecord(r, "Date") && isValidRecord(r, "Amount"))
                    .map(this::mapToTransaction)
                    .toList()
                    .stream();
        } catch (Exception e) {
            throw new CsvParsingException("Failed to parse Ally savings CSV", e);
        }
    }

    private Transaction mapToTransaction(CSVRecord csvRecord) {
        String description = csvRecord.isMapped("Description") ? csvRecord.get("Description") : "";
        String kind = csvRecord.isMapped("Type") ? csvRecord.get("Type") : "";

        return Transaction.builder()
                .description(description)
                .amount(parseAmount(csvRecord, "Amount").abs())
                .transactionDate(parseDate(csvRecord.get("Date")))
                .type(resolveType(kind, description))
                .build();
    }

    private TransactionType resolveType(String kind, String description) {
        if ("Withdrawal".equalsIgnoreCase(kind)) {
            return TransactionType.TRANSFER_OUT;
        }
        if (description.trim().equalsIgnoreCase("Interest Paid")) {
            return TransactionType.EARNINGS;
        }
        if (description.toLowerCase(Locale.ROOT).contains("transfer")) {
            return TransactionType.TRANSFER_IN;
        }
        return TransactionType.INCOME;
    }

    /** Parses the ISO local date column to UTC midnight; the Time column is deliberately ignored. */
    private OffsetDateTime parseDate(String dateStr) {
        return LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    }
}
