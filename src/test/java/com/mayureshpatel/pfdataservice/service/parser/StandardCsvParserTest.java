package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class StandardCsvParserTest {

    private final StandardCsvParser parser = new StandardCsvParser();

    @Test
    void parse_ShouldParseExpense() {
        String csv = "date,description,amount\n" +
                     "2023-10-01,Test Expense,-10.50";
        InputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        try (Stream<Transaction> transactionStream = parser.parse(1L, is)) {
            List<Transaction> result = transactionStream.toList();
            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getDate()).isEqualTo(LocalDate.of(2023, 10, 1));
            assertThat(t.getDescription()).isEqualTo("Test Expense");
            assertThat(t.getAmount()).isEqualByComparingTo("10.50");
            assertThat(t.getType()).isEqualTo(TransactionType.EXPENSE);
        }
    }

    @Test
    void parse_ShouldParseIncome() {
        String csv = "date,description,amount\n" +
                     "2023-10-02,Salary,5000.00";
        InputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        try (Stream<Transaction> transactionStream = parser.parse(1L, is)) {
            List<Transaction> result = transactionStream.toList();
            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(t.getAmount()).isEqualByComparingTo("5000.00");
        }
    }

    @Test
    void parse_ShouldHandleCurrencySymbolsAndCommas() {
        String csv = "date,description,amount\n" +
                     "2023-10-03,Bonus,\"$1,000.00\"";
        InputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        try (Stream<Transaction> transactionStream = parser.parse(1L, is)) {
            List<Transaction> result = transactionStream.toList();
            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getAmount()).isEqualByComparingTo("1000.00");
            assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
        }
    }
}