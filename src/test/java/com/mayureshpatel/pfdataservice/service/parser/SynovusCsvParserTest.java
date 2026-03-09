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

@DisplayName("SynovusCsvParser unit tests")
class SynovusCsvParserTest {

    private final SynovusCsvParser parser = new SynovusCsvParser();
    private static final Long ACCOUNT_ID = 1L;

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("getBankName() should return SYNOVUS")
    void getBankName_returnsSynovus() {
        assertThat(parser.getBankName()).isEqualTo(BankName.SYNOVUS);
    }

    @Nested
    @DisplayName("parse() — valid CSV")
    class ValidCsvTests {

        @Test
        @DisplayName("should return INCOME when credit > 0 and debit is 0 (net = credit + debit > 0)")
        void parse_creditOnly_returnsIncome() {
            // net = 500 + 0 = 500 → INCOME
            String csv = "Date,Description,Credit,Debit\n" +
                    "1/15/2025,Salary Deposit,500.00,0\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(t.getDescription()).isEqualTo("Salary Deposit");
        }

        @Test
        @DisplayName("should return EXPENSE when debit is negative and credit is 0 (net = credit + debit < 0)")
        void parse_debitNegative_returnsExpense() {
            // net = 0 + (-50) = -50 → EXPENSE
            String csv = "Date,Description,Credit,Debit\n" +
                    "1/20/2025,Grocery Store,0,-50.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("should return empty stream when CSV has only headers")
        void parse_headersOnly_returnsEmptyStream() {
            String csv = "Date,Description,Credit,Debit\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should skip rows where the date column is blank")
        void parse_blankDateRow_rowSkipped() {
            String csv = "Date,Description,Credit,Debit\n" +
                    ",Empty Date,100.00,0\n" +
                    "1/01/2025,Valid Date,200.00,0\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should parse multiple records and return all valid transactions")
        void parse_multipleRecords_returnsAll() {
            String csv = "Date,Description,Credit,Debit\n" +
                    "1/01/2025,Income,300.00,0\n" +
                    "1/02/2025,Expense,0,-75.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should parse the new Synovus format with quotes, metadata and footer")
        void parse_newFormat_parsesCorrectly() {
            String csv = "\"Plus Checking\",\"*******0744\"\n" +
                    "\"Balance as of 12/04/2025\",\"9499.150000\"\n" +
                    "\"Date\",\"Account\",\"Account Number\",\"Account Type\",\"Description\",\"Category\",\"Credit\",\"Debit\"\n" +
                    "\"11/28/2025\",\"Plus Checking\",\"*******0744\",\"Plus Checking\",\"GM-GMC DIVISION\",\"Income\",\"2914.46\",\"\"\n" +
                    "\"11/26/2025\",\"Plus Checking\",\"*******0744\",\"Plus Checking\",\"PAPER STMT FEE\",\"Fees\",\"\",\"-2.00\"\n" +
                    "\"\",\"\",\"\",\"\",\"Totals:\",\"9 items\",\"5826.86\",\"-4979.96\"\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDescription()).isEqualTo("GM-GMC DIVISION");
            assertThat(result.get(1).getDescription()).isEqualTo("PAPER STMT FEE");
            assertThat(result.get(1).getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("2.00"));
        }

        @Test
        @DisplayName("should parse tab-separated Synovus format correctly")
        void parse_tabSeparated_parsesCorrectly() {
            String tsv = "Plus Checking\t*******0744\t\t\t\t\t\n" +
                    "Balance as of 12/04/2025\t9499.15\t\t\t\t\t\n" +
                    "Date\tAccount\tAccount Number\tAccount Type\tDescription\tCategory\tCredit\tDebit\n" +
                    "11/28/25\tPlus Checking\t*******0744\tPlus Checking\tGM-GMC\tIncome\t2914.46\t\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(tsv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).isEqualTo("GM-GMC");
            assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("2914.46"));
        }

        @Test
        @DisplayName("should handle Byte Order Mark (BOM) at start of file")
        void parse_withBOM_parsesCorrectly() {
            String csv = "\uFEFFDate,Description,Credit,Debit\n" +
                    "1/01/2025,BOM Test,100.00,0\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).isEqualTo("BOM Test");
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
