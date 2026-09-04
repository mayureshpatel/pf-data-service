package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.apache.commons.csv.CSVRecord;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * Parses a bank-specific CSV export into a stream of transactions. Each implementing bank format
 * (Standard, Discover, Capital One, Synovus, Universal) provides its own header names, date
 * formats, and amount-sign conventions; {@link TransactionParserFactory} selects the right one by
 * {@link #getBankName()}. The default methods here are shared parsing helpers implementations can
 * reuse, not requirements every format needs.
 */
public interface TransactionParser {
    /**
     * Parses a CSV file into a stream of transactions. {@code accountId} is accepted for
     * interface-uniformity but individual implementations may not use it, since the transactions
     * they build aren't yet attached to an account at parse time.
     *
     * @param accountId   the account the parsed transactions will belong to
     * @param inputStream the CSV file's contents
     * @return the parsed transactions
     */
    Stream<Transaction> parse(Long accountId, InputStream inputStream);

    /**
     * The bank format this parser handles.
     *
     * @return the bank name
     */
    BankName getBankName();

    /**
     * Validates if a CSV record contains a non-empty value for a specified header.
     *
     * @param csvRecord the CSV record to validate
     * @param header    the header key to check
     * @return true if the record is valid, false otherwise
     */
    default boolean isValidRecord(CSVRecord csvRecord, String header) {
        return csvRecord.isMapped(header) && StringUtils.hasText(csvRecord.get(header));
    }

    /**
     * Parses a date string into an {@link OffsetDateTime}. The resulting offset comes entirely
     * from {@code dateTimeFormatter} -- this method applies no zone of its own. Every
     * implementation's formatter must resolve to UTC (e.g. via
     * {@code parseDefaulting(ChronoField.OFFSET_SECONDS, 0)}), matching this app's UTC-normalized
     * storage convention; a formatter with a real {@code .withZone(...)} override (a specific
     * timezone rather than a fixed UTC offset) will silently shift every date it parses, as
     * happened in {@code DiscoverCsvParser} (PF-197) -- confirm any new or changed formatter
     * resolves to UTC before relying on this method.
     *
     * @param dateStr           the date string to parse
     * @param dateTimeFormatter the date format to use; must resolve to UTC
     * @return the parsed {@link OffsetDateTime}
     */
    default OffsetDateTime parseDate(String dateStr, DateTimeFormatter dateTimeFormatter) {
        return ZonedDateTime.parse(dateStr, dateTimeFormatter).toOffsetDateTime();
    }

    /**
     * Configures the transaction type and amount based on the net amount.
     *
     * @param transaction the transaction to configure
     * @param netAmount   the net amount of the transaction
     * @return a new transaction with type and amount configured
     */
    default Transaction configureTransactionTypeAndAmount(Transaction transaction, BigDecimal netAmount) {
        if (netAmount.compareTo(BigDecimal.ZERO) >= 0) {
            return transaction.toBuilder()
                    .type(TransactionType.INCOME)
                    .amount(netAmount)
                    .build();
        } else {
            return transaction.toBuilder()
                    .type(TransactionType.EXPENSE)
                    .amount(netAmount.abs())
                    .build();
        }
    }

    /**
     * Configures the transaction type and amount based on the net amount for credit card accounts.
     * Positive amounts are treated as expenses (charges), negative amounts as transfers in (payments).
     *
     * @param transaction the transaction to configure
     * @param netAmount   the net amount of the transaction
     * @return a new transaction with type and amount configured
     */
    default Transaction configureCreditCardTransactionTypeAndAmount(Transaction transaction, BigDecimal netAmount) {
        if (netAmount.compareTo(BigDecimal.ZERO) >= 0) {
            // charges are positive in Discover/CapitalOne(debit-credit)
            return transaction.toBuilder()
                    .type(TransactionType.EXPENSE)
                    .amount(netAmount)
                    .build();
        } else {
            // payments are negative in Discover/CapitalOne(debit-credit)
            return transaction.toBuilder()
                    .type(TransactionType.TRANSFER_IN)
                    .amount(netAmount.abs())
                    .build();
        }
    }

    /**
     * Indicates if this parser is for a credit card account.
     *
     * @return true if it's a credit card account, false otherwise
     */
    default boolean isCreditCard() {
        return false;
    }

    /**
     * Parses an amount string from a CSV record into a {@link BigDecimal}. A header entirely
     * absent from the file's own header row is a structural problem with the file -- e.g. a bank
     * changing its export format to rename or split a required column, as happened to
     * {@code DiscoverCsvParser} pre-July-2022 (PF-198) -- and throws, rather than silently
     * resolving to zero and losing every affected transaction's amount without any error. A
     * header that exists but is blank for this specific row is a legitimately empty value and
     * still resolves to zero.
     *
     * @param csvRecord the CSV record containing the amount
     * @param header    the header key for the amount
     * @return the parsed {@link BigDecimal} amount, or zero if the column is blank for this row
     * @throws IllegalArgumentException if the column doesn't exist in the file at all, or its
     *                                   value can't be parsed as a number
     */
    default BigDecimal parseAmount(CSVRecord csvRecord, String header) {
        if (!csvRecord.isMapped(header)) {
            throw new IllegalArgumentException("Required column '" + header + "' is missing from this file.");
        }

        String stringVal = csvRecord.get(header);
        if (!StringUtils.hasText(stringVal)) {
            return BigDecimal.ZERO;
        }

        String amount = stringVal.replaceAll("[^0-9.-]", "");
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + stringVal, e);
        }
    }
}
