package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class DiscoverCsvParser implements TransactionParser {
    private static final String HEADER_DATE = "Trans. Date";
    private static final String HEADER_DESC = "Description";
    private static final String HEADER_AMOUNT = "Amount";

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[M/d/yyyy][MM/dd/yyyy][yyyy-MM-dd]")
            .toFormatter();

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
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            CSVParser csvParser = CSV_FORMAT.parse(reader);
            return csvParser.stream()
                    .filter(csvRecord -> isValidRecord(csvRecord, HEADER_DATE))
                    .map(this::parseTransaction)
                    .flatMap(Optional::stream)
                    .onClose(() -> {
                        try {
                            csvParser.close();
                            reader.close();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to close CSV parser resources", e);
                        }
                    });
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
     * @return the parsed Transaction or empty if invalid
     */
    private Optional<Transaction> parseTransaction(CSVRecord csvRecord) {
        try {
            Transaction transaction = new Transaction();

            transaction.setTransactionDate(parseDate(csvRecord.get(HEADER_DATE), DATE_FORMATTER));
            transaction.getMerchant().setOriginalName(csvRecord.get(HEADER_DESC));
            BigDecimal rawAmount = parseAmount(csvRecord, HEADER_AMOUNT);
            configureTransactionTypeAndAmount(transaction, rawAmount);
            transaction.setCategory(null);

            return Optional.of(transaction);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}