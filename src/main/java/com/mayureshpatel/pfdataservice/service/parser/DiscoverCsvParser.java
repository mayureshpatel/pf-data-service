package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
@Slf4j
public class DiscoverCsvParser implements TransactionParser {
    private static final String HEADER_DATE = "Trans. Date";
    private static final String HEADER_DESC = "Description";
    private static final String HEADER_AMOUNT = "Amount";

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[M/d/yyyy][MM/dd/yyyy][yyyy-MM-dd]")
            .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
            .toFormatter()
            .withZone(ZoneId.of("America/New_York"));

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .setIgnoreSurroundingSpaces(true)
            .get();

    @Override
    public BankName getBankName() {
        return BankName.DISCOVER;
    }

    @Override
    public boolean isCreditCard() {
        return true;
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
            throw new RuntimeException("Failed to parse Discover CSV", e);
        }
    }

    /**
     * Parses a CSV record into a Transaction object.
     *
     * @param csvRecord the CSV record to parse
     * @return the parsed Transaction
     */
    private Transaction parseTransaction(CSVRecord csvRecord) {
        Transaction transaction = Transaction.builder()
                .transactionDate(parseDate(csvRecord.get(HEADER_DATE), DATE_FORMATTER))
                .description(csvRecord.get(HEADER_DESC))
                .merchant(Merchant.builder()
                        .originalName(csvRecord.get(HEADER_DESC))
                        .build())
                .build();

        BigDecimal rawAmount = parseAmount(csvRecord, HEADER_AMOUNT);
        transaction = configureCreditCardTransactionTypeAndAmount(transaction, rawAmount);

        return transaction;
    }
}