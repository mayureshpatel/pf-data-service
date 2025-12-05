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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SynovusCsvParserTest {
    private final SynovusCsvParser parser = new SynovusCsvParser();

    @Test
    void shouldReturnCorrectBankName() {
        assertThat(parser.getBankName()).isEqualTo("SYNOVUS");
    }

    @Test
    void shouldParseRealFileFromResources() throws IOException {
        // Updated path to match your structure: src/test/resources/parser/synovus-example.csv
        String filePath = "/parser/synovus-example.csv";

        try (InputStream inputStream = getClass().getResourceAsStream(filePath)) {
            assertThat(inputStream)
                    .as("File '%s' not found in classpath. Check src/test/resources/parser/", filePath)
                    .isNotNull();

            List<Transaction> results = parser.parse(1L, inputStream);

            assertThat(results).hasSize(7);

            // Row 1: Income
            Transaction first = results.getFirst();
            assertThat(first.getDate()).isEqualTo(LocalDate.of(2024, 1, 31));
            assertThat(first.getDescription()).isEqualTo("My Job (Income)");
            assertThat(first.getAmount()).isEqualByComparingTo("2675.96");
            assertThat(first.getType()).isEqualTo(TransactionType.INCOME);

            // Row 2: Expense
            Transaction second = results.get(1);
            assertThat(second.getDescription()).isEqualTo("Power Bill (Uncategorized Expense)");
            assertThat(second.getAmount()).isEqualByComparingTo("78.85");
            assertThat(second.getType()).isEqualTo(TransactionType.EXPENSE);
        }
    }

    @Test
    void shouldParseIncomeTransactionCorrectly() {
        String csv = """
                Date,Account,Description,Check #,Category,Credit,Debit
                1/31/2024,Free Checking,My Job,,Income,2675.96,
                """;

        List<Transaction> result = parser.parse(1L, toInputStream(csv));

        assertThat(result).hasSize(1);
        Transaction t = result.getFirst();

        assertThat(t.getDate()).isEqualTo(LocalDate.of(2024, 1, 31));
        assertThat(t.getDescription()).isEqualTo("My Job (Income)");
        assertThat(t.getAmount()).isEqualByComparingTo("2675.96");
        assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void shouldParseExpenseTransactionWithNegativeDebit() {
        String csv = """
                Date,Account,Description,Check #,Category,Credit,Debit
                1/30/2024,Free Checking,Power Bill,,Uncategorized Expense,,-78.85
                """;

        List<Transaction> result = parser.parse(1L, toInputStream(csv));

        assertThat(result).hasSize(1);
        Transaction t = result.getFirst();

        assertThat(t.getDate()).isEqualTo(LocalDate.of(2024, 1, 30));
        assertThat(t.getDescription()).isEqualTo("Power Bill (Uncategorized Expense)");
        assertThat(t.getAmount()).isEqualByComparingTo("78.85");
        assertThat(t.getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void shouldParseDateVariations() {
        // Testing M/d/yyyy and MM/dd/yyyy and potential typos
        String csv = """
                Date,Account,Description,Category,Credit,Debit
                1/3/2024,Acct,Rent,Rent,,-2071.49
                10/12/2024,Acct,Future,Misc,,-10.00
                """;

        List<Transaction> result = parser.parse(1L, toInputStream(csv));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2024, 1, 3));
        assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2024, 10, 12));
    }

    @Test
    void shouldHandleMixedCreditAndDebitInSameRow() {
        // Logic: Credit (100) + Debit (-50) = 50 Income
        String csv = """
                Date,Account,Description,Category,Credit,Debit
                1/1/2024,Acct,Refund Offset,Misc,100.00,-50.00
                """;

        List<Transaction> result = parser.parse(1L, toInputStream(csv));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAmount()).isEqualByComparingTo("50.00");
        assertThat(result.getFirst().getType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void shouldIgnoreEmptyOrMalformedRows() {
        String csv = """
                Date,Account,Description,Category,Credit,Debit
                ,,,,,,
                not-a-date,Acct,Bad Row,Misc,,
                ,Acct,No Date,,,
                """;

        List<Transaction> result = parser.parse(1L, toInputStream(csv));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleMissingColumnsGracefully() {
        // Missing "Credit" and "Debit" headers entirely
        String csv = """
                Date,Description
                1/1/2024,Just a note
                """;

        List<Transaction> result = parser.parse(1L, toInputStream(csv));

        // Should default amounts to 0
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAmount()).isEqualByComparingTo("0");
        assertThat(result.getFirst().getType()).isEqualTo(TransactionType.INCOME); // 0 is >= 0
    }

    @Test
    void shouldHandleGarbageDataInAmountColumns() {
        String csv = """
                Date,Description,Credit,Debit
                1/1/2024,Garbage,N/A,Unknown
                """;

        List<Transaction> result = parser.parse(1L, toInputStream(csv));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAmount()).isEqualByComparingTo("0");
    }

    @Test
    void shouldThrowRuntimeExceptionOnIoError() throws IOException {
        // Mocking InputStream to throw exception on read
        InputStream brokenStream = mock(InputStream.class);
        when(brokenStream.read()).thenThrow(new IOException("Disk error"));
        // Need to mock read(byte[], int, int) as well since BufferedReader uses it
        when(brokenStream.read(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new IOException("Disk error"));

        assertThatThrownBy(() -> parser.parse(1L, brokenStream))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse Synovus CSV");
    }

    private InputStream toInputStream(String csvContent) {
        return new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
    }
}