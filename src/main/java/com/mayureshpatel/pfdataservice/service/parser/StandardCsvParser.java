package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

@Component
public class StandardCsvParser implements TransactionParser {
    @Override
    public BankName getBankName() {
        return BankName.STANDARD;
    }

    @Override
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            CSVParser csvParser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader);
            return csvParser.stream()
                    .map(csvRecord -> {
                        Transaction t = new Transaction();

                        String description = csvRecord.get("description");
                        String amountStr = csvRecord.get("amount").replace("$", "").replace(",", "");
                        BigDecimal amount = new BigDecimal(amountStr);
                        TransactionType type = amount.compareTo(BigDecimal.ZERO) < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;
                        t.setDescription(description);
                        t.setAmount(amount.abs());
                        t.setTransactionDate(OffsetDateTime.parse(csvRecord.get("date")));
                        t.setType(type);

                        return t;
                    })
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
            } catch (Exception ignored) {}
            throw new RuntimeException("Failed to parse Standard CSV", e);
        }
    }
}