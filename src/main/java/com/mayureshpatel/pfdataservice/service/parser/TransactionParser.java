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
     * Parses a date string into an {@link OffsetDateTime}.
     *
     * @param dateStr           the date string to parse
     * @param dateTimeFormatter the date format to use
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
     * Parses an amount string from a CSV record into a {@link BigDecimal}.
     *
     * @param csvRecord the CSV record containing the amount
     * @param header    the header key for the amount
     * @return the parsed {@link BigDecimal} amount or zero if not found or invalid
     */
    default BigDecimal parseAmount(CSVRecord csvRecord, String header) {
        if (!csvRecord.isMapped(header)) {
            return BigDecimal.ZERO;
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
