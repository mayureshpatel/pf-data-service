package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CapitalOneCsvParserTest {

    private final CapitalOneCsvParser parser = new CapitalOneCsvParser();

    @Test
    void shouldReturnCorrectBankName() {
        assertThat(parser.getBankName().name()).isEqualTo("CAPITAL_ONE");
    }

    @Test
    void shouldParseRealFileFromResources() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/parser/capital-example.csv")) {
            assertThat(inputStream)
                    .as("File '%s' not found in classpath", "/parser/capital-example.csv")
                    .isNotNull();

            try (Stream<Transaction> transactionStream = parser.parse(1L, inputStream)) {
                List<Transaction> results = transactionStream.toList();
                assertThat(results).isNotEmpty();

                Transaction first = results.getFirst();
                assertThat(first.getDate()).isEqualTo(LocalDate.of(2024, 1, 28));
                assertThat(first.getDescription()).isEqualTo("DOMINO'S 5706 (Dining)");
                assertThat(first.getAmount()).isEqualByComparingTo("7.53");
                assertThat(first.getType()).isEqualTo(TransactionType.EXPENSE);
            }
        }
    }

    @Test
    void shouldParseExpenseCorrectly() {
        String csv = """
                Transaction Date,Posted Date,Card No.,Description,Category,Debit,Credit
                1/26/2024,1/27/2024,8809,KROGER #431,Merchandise,34.34,
                """;

        try (Stream<Transaction> transactionStream = parser.parse(1L, toInputStream(csv))) {
            List<Transaction> results = transactionStream.toList();
            assertThat(results).hasSize(1);
            Transaction t = results.getFirst();

            assertThat(t.getDate()).isEqualTo(LocalDate.of(2024, 1, 26));
            assertThat(t.getDescription()).isEqualTo("KROGER #431 (Merchandise)");
            assertThat(t.getAmount()).isEqualByComparingTo("34.34");
            assertThat(t.getType()).isEqualTo(TransactionType.EXPENSE);
        }
    }

    @Test
    void shouldParsePaymentOrCreditCorrectly() {
        String csv = """
                Transaction Date,Posted Date,Card No.,Description,Category,Debit,Credit
                1/25/2024,1/26/2024,8809,PAYMENT - THANK YOU,Payment,,500.00
                """;

        try (Stream<Transaction> transactionStream = parser.parse(1L, toInputStream(csv))) {
            List<Transaction> results = transactionStream.toList();
            assertThat(results).hasSize(1);
            Transaction t = results.getFirst();

            assertThat(t.getDescription()).isEqualTo("PAYMENT - THANK YOU (Payment)");
            assertThat(t.getAmount()).isEqualByComparingTo("500.00");
            assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
        }
    }

    @Test
    void shouldHandleMissingCategory() {
        String csv = """
                Transaction Date,Posted Date,Card No.,Description,Category,Debit,Credit
                1/26/2024,1/27/2024,8809,MYSTERY CHARGE,,10.00,
                """;

        try (Stream<Transaction> transactionStream = parser.parse(1L, toInputStream(csv))) {
            List<Transaction> results = transactionStream.toList();
            Transaction t = results.getFirst();
            assertThat(t.getDescription()).isEqualTo("MYSTERY CHARGE");
        }
    }

    @Test
    void shouldIgnoreEmptyRows() {
        String csv = """
                Transaction Date,Description,Debit,Credit
                ,,,
                1/1/2024,Valid,10.00,
                """;
        try (Stream<Transaction> transactionStream = parser.parse(1L, toInputStream(csv))) {
            List<Transaction> results = transactionStream.toList();
            assertThat(results).hasSize(1);
        }
    }

    private InputStream toInputStream(String csvContent) {
        return new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
    }
}
