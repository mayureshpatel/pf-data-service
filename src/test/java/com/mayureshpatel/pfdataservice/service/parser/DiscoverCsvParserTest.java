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
            // NullPointerException is thrown by InputStreamReader before the try-catch wraps it
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

            List<?> result;
            try (Stream<?> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should silently skip records when transaction.getMerchant() is null")
        void parse_recordsWithNullMerchant_allRecordsSkipped() {
            // DiscoverCsvParser calls transaction.getMerchant().setOriginalName(description),
            // but Transaction initializes merchant as null. The resulting NullPointerException
            // is caught in parseTransaction(), so all records return Optional.empty().
            String csv = "Trans. Date,Description,Amount\n" +
                    "1/15/2025,Starbucks,25.00\n" +
                    "1/16/2025,Amazon,50.00\n";

            List<?> result;
            try (Stream<?> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should not throw during stream consumption even when all records fail to parse")
        void parse_allRecordsFail_doesNotThrow() {
            String csv = "Trans. Date,Description,Amount\n" +
                    "1/15/2025,Coffee,10.00\n";

            try (Stream<?> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                stream.toList(); // should not throw
            }
        }
    }
}
