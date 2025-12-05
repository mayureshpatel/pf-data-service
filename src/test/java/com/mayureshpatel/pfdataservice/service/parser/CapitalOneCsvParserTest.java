package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CapitalOneCsvParserTest {

    private final CapitalOneCsvParser parser = new CapitalOneCsvParser();

    @Test
    void shouldReturnCorrectBankName() {
        assertThat(parser.getBankName()).isEqualTo("CAPITAL_ONE");
    }

    @Test
    void shouldParseRealFileFromResources() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/parser/capital-example.csv")) {
            assertThat(inputStream)
                    .as("File '%s' not found in classpath", "/parser/capital-example.csv")
                    .isNotNull();

            List<Transaction> results = parser.parse(1L, inputStream);

            // Based on the snippet provided, there are roughly 10+ rows
            assertThat(results).isNotEmpty();

            // Verify Row 1: Domino's (Debit/Expense)
            // 1/28/2024, ..., DOMINO'S 5706, Dining, 7.53, (Credit empty)
            Transaction first = results.getFirst();
            assertThat(first.getDate()).isEqualTo(LocalDate.of(2024, 1, 28));
            assertThat(first.getDescription()).isEqualTo("DOMINO'S 5706 (Dining)");
            assertThat(first.getAmount()).isEqualByComparingTo("7.53");
            assertThat(first.getType()).isEqualTo(TransactionType.EXPENSE);
        }
    }

    @Test
    void shouldParseExpenseCorrectly() {
        // Capital One format: Debit is positive number
        String csv = """
                Transaction Date,Posted Date,Card No.,Description,Category,Debit,Credit
                1/26/2024,1/27/2024,8809,KROGER #431,Merchandise,34.34,
                """;

        List<Transaction> results = parser.parse(1L, toInputStream(csv));

        assertThat(results).hasSize(1);
        Transaction t = results.getFirst();

        assertThat(t.getDate()).isEqualTo(LocalDate.of(2024, 1, 26));
        assertThat(t.getDescription()).isEqualTo("KROGER #431 (Merchandise)");
        assertThat(t.getAmount()).isEqualByComparingTo("34.34");
        assertThat(t.getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void shouldParsePaymentOrCreditCorrectly() {
        // Test a Payment (Credit column has value)
        String csv = """
                Transaction Date,Posted Date,Card No.,Description,Category,Debit,Credit
                1/25/2024,1/26/2024,8809,PAYMENT - THANK YOU,Payment,,500.00
                """;

        List<Transaction> results = parser.parse(1L, toInputStream(csv));

        assertThat(results).hasSize(1);
        Transaction t = results.getFirst();

        assertThat(t.getDescription()).isEqualTo("PAYMENT - THANK YOU (Payment)");
        assertThat(t.getAmount()).isEqualByComparingTo("500.00");
        assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void shouldHandleMissingCategory() {
        String csv = """
                Transaction Date,Posted Date,Card No.,Description,Category,Debit,Credit
                1/26/2024,1/27/2024,8809,MYSTERY CHARGE,,10.00,
                """;

        List<Transaction> results = parser.parse(1L, toInputStream(csv));

        Transaction t = results.getFirst();
        // Description should not have "()" appended if category is missing
        assertThat(t.getDescription()).isEqualTo("MYSTERY CHARGE");
    }

    @Test
    void shouldIgnoreEmptyRows() {
        String csv = """
                Transaction Date,Description,Debit,Credit
                ,,,
                1/1/2024,Valid,10.00,
                """;
        List<Transaction> results = parser.parse(1L, toInputStream(csv));
        assertThat(results).hasSize(1);
    }

    private InputStream toInputStream(String csvContent) {
        return new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
    }
}