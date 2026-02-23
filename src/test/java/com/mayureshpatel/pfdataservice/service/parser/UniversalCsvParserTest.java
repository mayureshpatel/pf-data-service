package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UniversalCsvParser unit tests")
class UniversalCsvParserTest {

    private final UniversalCsvParser parser = new UniversalCsvParser();
    private static final Long ACCOUNT_ID = 1L;

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("getBankName() should return UNIVERSAL")
    void getBankName_returnsUniversal() {
        assertThat(parser.getBankName()).isEqualTo(BankName.UNIVERSAL);
    }

    @Nested
    @DisplayName("parse() — column detection failures")
    class ColumnDetectionFailureTests {

        @Test
        @DisplayName("should throw RuntimeException when no date column can be identified")
        void parse_noDateColumn_throwsRuntimeException() {
            String csv = "Description,Amount\n" +
                    "Grocery,50.00\n";

            assertThatThrownBy(() -> {
                try (Stream<?> s = parser.parse(ACCOUNT_ID, toStream(csv))) {
                    s.toList();
                }
            }).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to parse Universal CSV");
        }

        @Test
        @DisplayName("should throw RuntimeException when no description column can be identified")
        void parse_noDescriptionColumn_throwsRuntimeException() {
            String csv = "Date,Amount\n" +
                    "1/1/2025,50.00\n";

            assertThatThrownBy(() -> {
                try (Stream<?> s = parser.parse(ACCOUNT_ID, toStream(csv))) {
                    s.toList();
                }
            }).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to parse Universal CSV");
        }

        @Test
        @DisplayName("should throw RuntimeException when neither amount nor debit/credit columns are present")
        void parse_noAmountOrDebitCreditColumns_throwsRuntimeException() {
            String csv = "Date,Description\n" +
                    "1/1/2025,Grocery\n";

            assertThatThrownBy(() -> {
                try (Stream<?> s = parser.parse(ACCOUNT_ID, toStream(csv))) {
                    s.toList();
                }
            }).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to parse Universal CSV");
        }

        @Test
        @DisplayName("should throw NullPointerException when InputStream is null")
        void parse_nullInputStream_throwsNullPointerException() {
            // NullPointerException is thrown by InputStreamReader before the try-catch wraps it
            assertThatThrownBy(() -> parser.parse(ACCOUNT_ID, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("parse() — valid CSV headers only")
    class ValidHeadersOnlyTests {

        @Test
        @DisplayName("should return empty stream when CSV has only headers (amount column)")
        void parse_headersOnlyWithAmount_returnsEmptyStream() {
            String csv = "Date,Description,Amount\n";

            List<?> result;
            try (Stream<?> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty stream when CSV has only headers (debit/credit columns)")
        void parse_headersOnlyWithDebitCredit_returnsEmptyStream() {
            String csv = "Date,Description,Debit,Credit\n";

            List<?> result;
            try (Stream<?> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("parse() — record-level filtering")
    class RecordFilteringTests {

        @Test
        @DisplayName("should skip zero-amount transactions")
        void parse_zeroAmountRecord_recordSkipped() {
            String csv = "Date,Description,Amount\n" +
                    "1/1/2025,Pending Auth,0.00\n";

            List<?> result;
            try (Stream<?> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should use Post Date as transaction date when Date column is absent")
        void parse_postDateFallback_parsesSuccessfully() {
            // When "Date" column is missing but "Post Date" is present,
            // identifyColumns() falls back to use postDateCol as dateCol.
            // Records with valid post date should succeed.
            String csv = "Post Date,Description,Amount\n" +
                    "1/1/2025,Test Txn,0.00\n";

            // zero-amount is filtered, so result is empty but no exception thrown
            List<?> result;
            try (Stream<?> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).isEmpty();
        }
    }
}
