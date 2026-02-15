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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscoverCsvParserTest {

    private final DiscoverCsvParser parser = new DiscoverCsvParser();

    @Test
    void shouldReturnCorrectBankName() {
        assertThat(parser.getBankName().name()).isEqualTo("DISCOVER");
    }

    @Test
    void shouldParseRealFileFromResources() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/parser/discover-example.csv")) {
            assertThat(inputStream)
                    .as("File '%s' not found in classpath", "/parser/discover-example.csv")
                    .isNotNull();

            try (Stream<Transaction> transactionStream = parser.parse(1L, inputStream)) {
                List<Transaction> results = transactionStream.toList();
                assertThat(results).isNotEmpty();

                Transaction payment = results.getFirst();
                assertThat(payment.getDate()).isEqualTo(LocalDate.of(2024, 1, 2));
                assertThat(payment.getDescription()).isEqualTo("INTERNET PAYMENT - THANK YOU (Payments and Credits)");
                assertThat(payment.getAmount()).isEqualByComparingTo("843");
                assertThat(payment.getType()).isEqualTo(TransactionType.INCOME);

                Transaction expense = results.get(1);
                assertThat(expense.getDate()).isEqualTo(LocalDate.of(2024, 1, 5));
                assertThat(expense.getDescription()).isEqualTo("DUNKIN #302918 Q35 ROSWELL GA (Restaurants)");
                assertThat(expense.getAmount()).isEqualByComparingTo("6.44");
                assertThat(expense.getType()).isEqualTo(TransactionType.EXPENSE);
            }
        }
    }

    @Test
    void shouldParseExpenseCorrectly() {
        String csv = """
                Trans. Date,Post Date,Description,Amount,Category
                1/6/2024,1/6/2024,PUBLIX #626,48.32,Supermarkets
                """;

        try (Stream<Transaction> transactionStream = parser.parse(1L, toInputStream(csv))) {
            List<Transaction> result = transactionStream.toList();
            assertThat(result).hasSize(1);
            Transaction t = result.getFirst();
            assertThat(t.getDate()).isEqualTo(LocalDate.of(2024, 1, 6));
            assertThat(t.getDescription()).isEqualTo("PUBLIX #626 (Supermarkets)");
            assertThat(t.getAmount()).isEqualByComparingTo("48.32");
            assertThat(t.getType()).isEqualTo(TransactionType.EXPENSE);
        }
    }

    @Test
    void shouldParsePaymentCorrectly() {
        String csv = """
                Trans. Date,Post Date,Description,Amount,Category
                1/2/2024,1/2/2024,PAYMENT,-843,Payments
                """;

        try (Stream<Transaction> transactionStream = parser.parse(1L, toInputStream(csv))) {
            List<Transaction> result = transactionStream.toList();
            assertThat(result).hasSize(1);
            Transaction t = result.getFirst();
            assertThat(t.getDescription()).isEqualTo("PAYMENT (Payments)");
            assertThat(t.getAmount()).isEqualByComparingTo("843");
            assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
        }
    }

    @Test
    void shouldHandleMissingCategory() {
        String csv = """
                Trans. Date,Post Date,Description,Amount,Category
                1/5/2024,1/5/2024,MYSTERY,10.00,
                """;

        try (Stream<Transaction> transactionStream = parser.parse(1L, toInputStream(csv))) {
            List<Transaction> result = transactionStream.toList();
            Transaction t = result.getFirst();
            assertThat(t.getDescription()).isEqualTo("MYSTERY");
        }
    }

    @Test
    void shouldHandleZeroAmount() {
        String csv = """
                Trans. Date,Post Date,Description,Amount,Category
                1/5/2024,1/5/2024,HOLD,0.00,
                """;

        try (Stream<Transaction> transactionStream = parser.parse(1L, toInputStream(csv))) {
            List<Transaction> result = transactionStream.toList();
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getFirst().getType()).isEqualTo(TransactionType.EXPENSE);
        }
    }

    @Test
    void shouldIgnoreMalformedRows() {
        String csv = """
                Trans. Date,Post Date,Description,Amount,Category
                ,,,,,
                NotADate,1/1/2024,Desc,100,Cat
                """;

        try (Stream<Transaction> transactionStream = parser.parse(1L, toInputStream(csv))) {
            List<Transaction> result = transactionStream.toList();
            assertThat(result).isEmpty();
        }
    }

    @Test
    void shouldThrowRuntimeExceptionOnIoError() throws IOException {
        InputStream brokenStream = mock(InputStream.class);
        when(brokenStream.read()).thenThrow(new IOException("Disk failure"));
        when(brokenStream.read(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new IOException("Disk failure"));

        assertThatThrownBy(() -> parser.parse(1L, brokenStream))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse Discover CSV");
    }

    private InputStream toInputStream(String csvContent) {
        return new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
    }
}
