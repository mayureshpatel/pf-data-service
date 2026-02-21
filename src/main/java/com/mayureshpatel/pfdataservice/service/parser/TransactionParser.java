package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.apache.commons.csv.CSVRecord;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public interface TransactionParser {
    Stream<Transaction> parse(Long accountId, InputStream inputStream);

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
        return OffsetDateTime.parse(dateStr, dateTimeFormatter);
    }

    /**
     * Configures the transaction type and amount based on the net amount.
     *
     * @param transaction the transaction to configure
     * @param netAmount   the net amount of the transaction
     */
    default void configureTransactionTypeAndAmount(Transaction transaction, BigDecimal netAmount) {
        if (netAmount.compareTo(BigDecimal.ZERO) >= 0) {
            transaction.setType(TransactionType.INCOME);
            transaction.setAmount(netAmount);
        } else {
            transaction.setType(TransactionType.EXPENSE);
            transaction.setAmount(netAmount.abs());
        }
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
            return BigDecimal.ZERO;
        }
    }
}