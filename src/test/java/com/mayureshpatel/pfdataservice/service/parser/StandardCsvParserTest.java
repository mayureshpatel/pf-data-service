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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StandardCsvParser unit tests")
class StandardCsvParserTest {

    private final StandardCsvParser parser = new StandardCsvParser();
    private static final Long ACCOUNT_ID = 1L;

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("getBankName() should return STANDARD")
    void getBankName_returnsStandard() {
        assertThat(parser.getBankName()).isEqualTo(BankName.STANDARD);
    }

    @Nested
    @DisplayName("parse() — valid CSV")
    class ValidCsvTests {

        @Test
        @DisplayName("should return EXPENSE transaction when amount is negative")
        void parse_negativeAmount_returnsExpenseTransaction() {
            String csv = "date,description,amount\n" +
                    "2025-01-15T00:00:00Z,Coffee Shop,-25.50\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("25.50"));
            assertThat(t.getDescription()).isEqualTo("Coffee Shop");
        }

        @Test
        @DisplayName("should return INCOME transaction when amount is positive")
        void parse_positiveAmount_returnsIncomeTransaction() {
            String csv = "date,description,amount\n" +
                    "2025-01-20T00:00:00Z,Paycheck,3000.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
        }

        @Test
        @DisplayName("should return INCOME transaction when amount is zero")
        void parse_zeroAmount_returnsIncomeTransaction() {
            String csv = "date,description,amount\n" +
                    "2025-01-01T00:00:00Z,Zero Txn,0.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("should strip dollar signs and commas from amount")
        void parse_amountWithDollarAndCommas_parsedCorrectly() {
            // Quote the amount so the CSV parser treats it as a single field (otherwise
            // the comma in "-$1,250.00" would split it into two separate columns)
            String csv = "date,description,amount\n" +
                    "2025-01-01T00:00:00Z,Groceries,\"-$1,250.00\"\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1250.00"));
            assertThat(result.get(0).getType()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("should return empty stream when CSV has only headers")
        void parse_headersOnly_returnsEmptyStream() {
            String csv = "date,description,amount\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return all transactions when multiple records are present")
        void parse_multipleRecords_returnsAllTransactions() {
            String csv = "date,description,amount\n" +
                    "2025-01-01T00:00:00Z,Expense,-50.00\n" +
                    "2025-01-02T00:00:00Z,Income,200.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should correctly set the transactionDate from ISO-8601 string")
        void parse_isoDateString_setsTransactionDate() {
            String csv = "date,description,amount\n" +
                    "2025-06-15T12:30:00Z,Test,100.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTransactionDate())
                    .isEqualTo(OffsetDateTime.parse("2025-06-15T12:30:00Z"));
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
