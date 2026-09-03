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
import java.time.OffsetDateTime;
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
     * (negative -> expense, non-negative -> income) and is normalized to an absolute value.
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
                .transactionDate(OffsetDateTime.parse(csvRecord.get("date")))
                .type(type)
                .build();
    }
}
