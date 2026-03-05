package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UniversalCsvParser Unit Tests")
class UniversalCsvParserTest {

    private final UniversalCsvParser parser = new UniversalCsvParser();

    @Nested
    @DisplayName("identifyColumns")
    class IdentifyColumnsTests {
        @Test
        @DisplayName("should identify columns correctly with standard names")
        void shouldMapStandardHeaders() {
            String csv = "Date,Description,Amount\n03/01/2026,Test,100.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                List<Transaction> txns = result.toList();
                assertEquals(1, txns.size());
                assertEquals("Test", txns.get(0).getDescription());
            }
        }

        @Test
        @DisplayName("should identify columns with varied names matching regex patterns")
        void shouldMapVariedHeaders() {
            String csv = "Trans Date,Memo,Debit($),Credit($)\n03/01/2026,Purchase,50.00,0.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                Transaction t = result.findFirst().orElseThrow();
                assertEquals("Purchase", t.getDescription());
                assertEquals(new BigDecimal("50.00"), t.getAmount());
            }

            String csv2 = "date,payee,amount ($)\n03/01/2026,Store,10.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv2.getBytes()))) {
                Transaction t = result.findFirst().orElseThrow();
                assertEquals("Store", t.getDescription());
            }
        }

        @Test
        @DisplayName("should fallback to Post Date if Date is missing")
        void shouldFallbackToPostDate() {
            String csv = "Post Date,Description,Amount\n03/01/2026,Test,100.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                assertEquals(1, result.count());
            }
        }

        @Test
        @DisplayName("should identify post date correctly")
        void shouldMapPostDate() {
            String csv = "Date,Posted Date,Description,Amount\n03/01/2026,03/02/2026,Test,10.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                Transaction t = result.findFirst().orElseThrow();
                assertNotNull(t.getPostDate());
            }
        }

        @Test
        @DisplayName("should throw error if Date column is missing")
        void shouldThrowOnMissingDate() {
            String csv = "Description,Amount\nTest,100.00";
            assertThrows(RuntimeException.class, () -> parser.parse(1L, new ByteArrayInputStream(csv.getBytes())));
        }

        @Test
        @DisplayName("should throw error if Description column is missing")
        void shouldThrowOnMissingDesc() {
            String csv = "Date,Amount\n03/01/2026,100.00";
            assertThrows(RuntimeException.class, () -> parser.parse(1L, new ByteArrayInputStream(csv.getBytes())));
        }
    }

    @Nested
    @DisplayName("parseRecord logic")
    class ParseRecordTests {
        @Test
        @DisplayName("should handle Debit/Credit split columns")
        void shouldHandleDebitCredit() {
            String csv = "Date,Description,Debit,Credit\n" +
                         "03/01/2026,Buy,50.00,\n" +
                         "03/02/2026,Salary,,1000.00";
            
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                List<Transaction> txns = result.toList();
                assertEquals(2, txns.size());
                assertEquals(new BigDecimal("50.00"), txns.get(0).getAmount());
                assertEquals(TransactionType.EXPENSE, txns.get(0).getType());
                assertEquals(new BigDecimal("1000.00"), txns.get(1).getAmount());
                assertEquals(TransactionType.INCOME, txns.get(1).getType());
            }
        }

        @Test
        @DisplayName("should handle single Amount column with negative values")
        void shouldHandleSingleAmount() {
            String csv = "Date,Description,Amount\n" +
                         "03/01/2026,Expense,-25.00\n" +
                         "03/02/2026,Income,100.00";
            
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                List<Transaction> txns = result.toList();
                assertEquals(new BigDecimal("25.00"), txns.get(0).getAmount());
                assertEquals(TransactionType.EXPENSE, txns.get(0).getType());
                assertEquals(TransactionType.INCOME, txns.get(1).getType());
            }
        }

        @Test
        @DisplayName("should skip zero amount rows")
        void shouldSkipZeros() {
            String csv = "Date,Description,Amount\n03/01/2026,Zero,0.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                assertEquals(0, result.count());
            }
        }

        @Test
        @DisplayName("should handle various date formats")
        void shouldSupportDateFormats() {
            String[] dates = {"3/1/2026", "03/01/2026", "2026-03-01", "1/3/2026", "01/03/2026"};
            for (String date : dates) {
                String csv = "Date,Description,Amount\n" + date + ",Test,10.00";
                try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                    assertEquals(1, result.count(), "Failed for date: " + date);
                }
            }
        }
    }

    @Nested
    @DisplayName("edge cases and errors")
    class EdgeCaseTests {
        @Test
        @DisplayName("should handle malformed financials")
        void shouldHandleMalformedFinancials() {
            String csv = "Date,Description,Amount\n" +
                         "03/01/2026,Empty,\n" +
                         "03/01/2026,Bad,ABC\n" +
                         "03/01/2026,Null,null";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                assertEquals(0, result.count());
            }
        }

        @Test
        @DisplayName("should handle only Debit column being mapped")
        void shouldHandleOnlyDebitMapping() {
            String csv = "Date,Description,debit\n03/01/2026,Test,50.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                Transaction t = result.findFirst().orElseThrow();
                assertEquals(new BigDecimal("50.00"), t.getAmount());
                assertEquals(TransactionType.EXPENSE, t.getType());
            }
        }

        @Test
        @DisplayName("should handle only Credit column being mapped")
        void shouldHandleOnlyCreditMapping() {
            String csv = "Date,Description,credit\n03/01/2026,Test,50.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                Transaction t = result.findFirst().orElseThrow();
                assertEquals(new BigDecimal("50.00"), t.getAmount());
                assertEquals(TransactionType.INCOME, t.getType());
            }
        }

        @Test
        @DisplayName("should handle debit/credit split where both are present but one is zero")
        void shouldHandleSplitWithZeros() {
            String csv = "Date,Description,Debit,Credit\n03/01/2026,Test,0.00,100.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                Transaction t = result.findFirst().orElseThrow();
                assertEquals(new BigDecimal("100.00"), t.getAmount());
                assertEquals(TransactionType.INCOME, t.getType());
            }
        }

        @Test
        @DisplayName("should handle debit/credit split where only debit is positive")
        void shouldHandleSplitWithDebit() {
            String csv = "Date,Description,Debit,Credit\n03/01/2026,Test,50.00,0.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                Transaction t = result.findFirst().orElseThrow();
                assertEquals(new BigDecimal("50.00"), t.getAmount());
                assertEquals(TransactionType.EXPENSE, t.getType());
            }
        }

        @Test
        @DisplayName("should skip rows where required columns are null in record")
        void shouldSkipRowsWithNullValues() {
            String csv = "Date,Description,Amount\n,Test,10.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                assertEquals(0, result.count());
            }
        }

        @Test
        @DisplayName("should handle unknown date format")
        void shouldHandleUnknownDate() {
            String csv = "Date,Description,Amount\n03-Mar-2026,Test,10.00";
            try (Stream<Transaction> result = parser.parse(1L, new ByteArrayInputStream(csv.getBytes()))) {
                assertEquals(0, result.count());
            }
        }
    }
}
