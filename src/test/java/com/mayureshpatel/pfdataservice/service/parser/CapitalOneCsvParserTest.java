package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CapitalOneCsvParser unit tests")
class CapitalOneCsvParserTest {

    private final CapitalOneCsvParser parser = new CapitalOneCsvParser();
    private static final Long ACCOUNT_ID = 1L;

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("getBankName() should return CAPITAL_ONE")
    void getBankName_returnsCapitalOne() {
        assertThat(parser.getBankName()).isEqualTo(BankName.CAPITAL_ONE);
    }

    @Nested
    @DisplayName("parse() — valid CSV")
    class ValidCsvTests {

        @Test
        @DisplayName("should return EXPENSE when debit > 0 and credit is empty (net = credit - debit < 0)")
        void parse_debitOnly_returnsExpense() {
            // net = 0 - 50 = -50 → EXPENSE
            String csv = "Transaction Date,Description,Debit,Credit\n" +
                    "2025-01-15,Coffee Shop,50.00,\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(t.getDescription()).isEqualTo("Coffee Shop");
        }

        @Test
        @DisplayName("should return INCOME when credit > 0 and debit is empty (net = credit - debit > 0)")
        void parse_creditOnly_returnsIncome() {
            // net = 1000 - 0 = 1000 → INCOME
            String csv = "Transaction Date,Description,Debit,Credit\n" +
                    "2025-01-20,Paycheck,,1000.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("should return empty stream when CSV has only headers")
        void parse_headersOnly_returnsEmptyStream() {
            String csv = "Transaction Date,Description,Debit,Credit\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should skip rows where the date column is blank")
        void parse_blankDateRow_rowSkipped() {
            String csv = "Transaction Date,Description,Debit,Credit\n" +
                    ",Empty Date,10.00,\n" +
                    "2025-01-01,Valid Date,5.00,\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            // Only the valid-date row should pass the isValidRecord filter
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should handle multiple date formats (yyyy-MM-dd, M/d/yyyy, MM/dd/yyyy)")
        void parse_variousDateFormats_parsedSuccessfully() {
            String csv = "Transaction Date,Description,Debit,Credit\n" +
                    "2025-01-15,ISO Format,25.00,\n" +
                    "1/20/2025,US Short Format,,50.00\n" +
                    "01/25/2025,US Long Format,15.00,\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("should return correct count when multiple records exist")
        void parse_multipleRecords_returnsAllValidTransactions() {
            String csv = "Transaction Date,Description,Debit,Credit\n" +
                    "2025-01-01,Expense A,100.00,\n" +
                    "2025-01-02,Income B,,200.00\n" +
                    "2025-01-03,Expense C,50.00,\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("parse() — invalid input")
    class InvalidInputTests {

        @Test
        @DisplayName("should throw NullPointerException when InputStream is null")
        void parse_nullInputStream_throwsNullPointerException() {
            // NullPointerException is thrown by InputStreamReader before the try-catch wraps it
            assertThatThrownBy(() -> parser.parse(ACCOUNT_ID, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
