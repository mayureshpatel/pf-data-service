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

@DisplayName("DiscoverCsvParser unit tests")
class DiscoverCsvParserTest {

    private final DiscoverCsvParser parser = new DiscoverCsvParser();
    private static final Long ACCOUNT_ID = 1L;

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("getBankName() should return DISCOVER")
    void getBankName_returnsDiscover() {
        assertThat(parser.getBankName()).isEqualTo(BankName.DISCOVER);
    }

    @Nested
    @DisplayName("parse() — setup error")
    class SetupErrorTests {

        @Test
        @DisplayName("should throw NullPointerException when InputStream is null")
        void parse_nullInputStream_throwsNullPointerException() {
            assertThatThrownBy(() -> parser.parse(ACCOUNT_ID, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("parse() — valid CSV")
    class ValidCsvTests {

        @Test
        @DisplayName("should return empty stream when CSV has only headers")
        void parse_headersOnly_returnsEmptyStream() {
            String csv = "Trans. Date,Description,Amount\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should parse positive amount as EXPENSE for credit card")
        void parse_positiveAmount_returnsExpenseTransaction() {
            String csv = "Trans. Date,Description,Amount\n" +
                    "1/15/2025,Starbucks,25.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(t.getDescription()).isEqualTo("Starbucks");
        }

        @Test
        @DisplayName("should parse negative amount as TRANSFER_IN for credit card")
        void parse_negativeAmount_returnsTransferInTransaction() {
            String csv = "Trans. Date,Description,Amount\n" +
                    "1/2/2025,INTERNET PAYMENT - THANK YOU,-843.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getType()).isEqualTo(TransactionType.TRANSFER_IN);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("843.00"));
        }

        @Test
        @DisplayName("should set merchant originalName from description")
        void parse_validRecord_setsMerchantOriginalName() {
            String csv = "Trans. Date,Description,Amount\n" +
                    "1/15/2025,KROGER #431 ROSWELL GA,48.32\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMerchant()).isNotNull();
            assertThat(result.get(0).getMerchant().getOriginalName()).isEqualTo("KROGER #431 ROSWELL GA");
        }

        @Test
        @DisplayName("should parse multiple records successfully")
        void parse_multipleRecords_returnsAllTransactions() {
            String csv = "Trans. Date,Description,Amount\n" +
                    "1/15/2025,Starbucks,25.00\n" +
                    "1/16/2025,Amazon,50.00\n" +
                    "1/17/2025,Payment,-500.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("should handle multiple date formats (M/d/yyyy, MM/dd/yyyy, yyyy-MM-dd)")
        void parse_variousDateFormats_parsedSuccessfully() {
            String csv = "Trans. Date,Description,Amount\n" +
                    "1/5/2025,Short Date,10.00\n" +
                    "01/15/2025,Padded Date,20.00\n" +
                    "2025-01-20,ISO Date,30.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("should skip rows where the date column is blank")
        void parse_blankDateRow_rowSkipped() {
            String csv = "Trans. Date,Description,Amount\n" +
                    ",Empty Date,10.00\n" +
                    "1/15/2025,Valid Date,5.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).isEqualTo("Valid Date");
        }

        @Test
        @DisplayName("should set category to null for all parsed transactions")
        void parse_validRecord_categoryIsNull() {
            String csv = "Trans. Date,Description,Amount\n" +
                    "1/15/2025,Coffee,5.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isNull();
        }
    }

    @Nested
    @DisplayName("parse() — real CSV file")
    class RealCsvTests {

        @Test
        @DisplayName("should parse the Discover example CSV file successfully")
        void parse_discoverExampleCsv_parsesAllRecords() {
            InputStream csvStream = getClass().getResourceAsStream("/parser/discover-example.csv");
            assertThat(csvStream).isNotNull();

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, csvStream)) {
                result = stream.toList();
            }

            assertThat(result).hasSize(38);
        }
    }
}
