package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StandardCsvParser implements TransactionParser {
    @Override
    public String getBankName() {
        return "STANDARD";
    }

    @Override
    public List<Transaction> parse(Long accountId, InputStream inputStream) {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(bufferedReader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

            for (CSVRecord csvRecord : csvParser) {
                Transaction t = new Transaction();

                t.setDescription(csvRecord.get("description"));
                t.setAmount(new BigDecimal(csvRecord.get("amount")));
                t.setDate(LocalDate.parse(csvRecord.get("date")));
                t.setType(TransactionType.EXPENSE);

                transactions.add(t);
            }
        } catch(Exception e) {
            throw new RuntimeException("Failed to parse Standard CSV", e);
        }

        return transactions;
    }
}
