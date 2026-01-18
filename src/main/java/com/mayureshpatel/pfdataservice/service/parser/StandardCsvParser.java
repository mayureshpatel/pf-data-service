package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
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
import java.util.ArrayList;
import java.util.List;

@Component
public class StandardCsvParser implements TransactionParser {
    @Override
    public String getBankName() {
        return "STANDARD";
    }

    @Override
    public List<Transaction> parse(Long accountId, InputStream inputStream) {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(bufferedReader)) {

            for (CSVRecord csvRecord : csvParser) {
                Transaction t = new Transaction();

                String description = csvRecord.get("description");
                String amountStr = csvRecord.get("amount").replace("$", "").replace(",", "");
                BigDecimal amount = new BigDecimal(amountStr);

                TransactionType type = amount.compareTo(BigDecimal.ZERO) < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;

                t.setDescription(description);
                t.setAmount(amount.abs());
                t.setDate(LocalDate.parse(csvRecord.get("date")));
                t.setType(type);

                transactions.add(t);
            }
        } catch(Exception e) {
            throw new RuntimeException("Failed to parse Standard CSV", e);
        }

        return transactions;
    }
}