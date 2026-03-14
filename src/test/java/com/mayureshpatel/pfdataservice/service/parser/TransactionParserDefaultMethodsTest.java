package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionParser interface default methods unit tests")
class TransactionParserDefaultMethodsTest {

    private final TransactionParser parser = new TransactionParser() {
        @Override
        public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
            return Stream.empty();
        }

        @Override
        public BankName getBankName() {
            return BankName.UNIVERSAL;
        }
    };

    @Test
    @DisplayName("isValidRecord() should validate mapped and non-empty headers")
    void isValidRecord_variousScenarios() throws Exception {
        String csv = "Header1,Header2\nValue1,";
        CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new StringReader(csv));
        CSVRecord record = csvParser.iterator().next();

        assertThat(parser.isValidRecord(record, "Header1")).isTrue();
        assertThat(parser.isValidRecord(record, "Header2")).isFalse();
        assertThat(parser.isValidRecord(record, "NonExistent")).isFalse();
    }

    @Test
    @DisplayName("parseDate() should parse valid date strings")
    void parseDate_validString_returnsOffsetDateTime() {
        String dateStr = "2025-01-15T10:00:00Z";
        OffsetDateTime result = parser.parseDate(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        assertThat(result.getYear()).isEqualTo(2025);
    }

    @Test
    @DisplayName("configureTransactionTypeAndAmount() should handle positive and negative amounts")
    void configureTransactionTypeAndAmount_variousAmounts() {
        Transaction income = new Transaction();
        parser.configureTransactionTypeAndAmount(income, new BigDecimal("100.50"));
        assertThat(income.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(income.getAmount()).isEqualByComparingTo("100.50");

        Transaction expense = new Transaction();
        parser.configureTransactionTypeAndAmount(expense, new BigDecimal("-50.25"));
        assertThat(expense.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(expense.getAmount()).isEqualByComparingTo("50.25");
    }

    @Test
    @DisplayName("parseAmount() should handle various numeric strings")
    void parseAmount_variousStrings() throws Exception {
        String csv = "Amount,Other\n" +
                "\"$1,234.56\",x\n" +
                "-100.00,x\n" +
                "invalid,x\n" +
                ",x\n";
        CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new StringReader(csv));
        
        List<CSVRecord> records = csvParser.getRecords();
        
        assertThat(parser.parseAmount(records.get(0), "Amount")).isEqualByComparingTo("1234.56");
        assertThat(parser.parseAmount(records.get(1), "Amount")).isEqualByComparingTo("-100.00");
        assertThat(parser.parseAmount(records.get(2), "Amount")).isEqualByComparingTo("0");
        assertThat(parser.parseAmount(records.get(3), "Amount")).isEqualByComparingTo("0"); // Empty string case
        assertThat(parser.parseAmount(records.get(0), "NonExistent")).isEqualByComparingTo("0");
    }
}
