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
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies {@code DiscoverCsvParser}'s single-Amount-column and legacy Debit/Credit parsing, one {@code @Nested} class per input shape below. */
@DisplayName("DiscoverCsvParser unit tests")
class DiscoverCsvParserTest {

    private static final String CSV_HEADER = "Trans. Date,Description,Amount\n";

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

    /** A null input stream fails fast with {@link NullPointerException} before any parsing begins. */
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

    /**
     * A credit-card amount column is sign-inverted from a bank account's: a positive amount is a
     * charge (EXPENSE) and a negative one is a payment or refund (INCOME, never TRANSFER_IN --
     * same PF-829 reasoning as {@link CapitalOneCsvParserTest}, since this parser likewise can't
     * tell a linked-account payment from a merchant refund). The transaction date parses as UTC
     * midnight regardless of server timezone (PF-197), blank-date rows are skipped, and the
     * category is always left null (categorization happens elsewhere).
     */
    @Nested
    @DisplayName("parse() — valid CSV")
    class ValidCsvTests {

        @Test
        @DisplayName("should return empty stream when CSV has only headers")
        void parse_headersOnly_returnsEmptyStream() {
            String csv = CSV_HEADER;

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should parse positive amount as EXPENSE for credit card")
        void parse_positiveAmount_returnsExpenseTransaction() {
            String csv = CSV_HEADER +
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
        @DisplayName("should parse negative amount as INCOME for credit card, not TRANSFER_IN (PF-829)")
        void parse_negativeAmount_returnsIncomeTransaction() {
            // a negative amount here is a payment or a merchant refund -- either way, this parser
            // has no way to tell them apart or confirm a real transfer's other half exists, so it
            // must not pre-emptively classify it as a transfer (see PF-829: doing so made a real
            // transfer un-matchable, and permanently hid real refunds from every total)
            String csv = CSV_HEADER +
                    "1/2/2025,INTERNET PAYMENT - THANK YOU,-843.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("843.00"));
        }

        @Test
        @DisplayName("should parse multiple records successfully")
        void parse_multipleRecords_returnsAllTransactions() {
            String csv = CSV_HEADER +
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
            String csv = CSV_HEADER +
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
            String csv = CSV_HEADER +
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
        @DisplayName("should parse the transaction date as UTC midnight, not shifted by a hardcoded timezone (PF-197)")
        void parse_transactionDate_isUtcMidnight() {
            String csv = CSV_HEADER +
                    "3/15/2025,Coffee,5.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTransactionDate())
                    .isEqualTo(OffsetDateTime.of(2025, 3, 15, 0, 0, 0, 0, ZoneOffset.UTC));
        }

        @Test
        @DisplayName("should set category to null for all parsed transactions")
        void parse_validRecord_categoryIsNull() {
            String csv = CSV_HEADER +
                    "1/15/2025,Coffee,5.00\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isNull();
        }
    }

    /**
     * Parses two real example export files from test resources end-to-end: the current format,
     * and the pre-July-2022 Debit/Credit-column format (PF-198: every parsed amount must be
     * genuinely non-zero, not silently zeroed by a missing-column bug). The fixture referenced by
     * the second test was moved into the already-tracked {@code parser/} resource directory as
     * part of PF-821, after the original path (an untracked local-only {@code sample-imports/}
     * directory) returned null on a clean CI checkout and failed this test outright.
     */
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

        @Test
        @DisplayName("should parse the pre-July-2022 Debit/Credit-column format with correct, non-zero amounts (PF-198)")
        void parse_preJuly2022DiscoverFormat_parsesNonZeroAmounts() {
            // PF-821: was /sample-imports/Discover-2022-06.csv, an untracked local-only directory
            // -- getResourceAsStream returned null on a clean CI checkout, failing this assertion
            // immediately. Moved into the already-tracked parser/ fixture directory alongside this
            // class's other real-format example CSVs.
            InputStream csvStream = getClass().getResourceAsStream("/parser/discover-old-example.csv");
            assertThat(csvStream).isNotNull();

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, csvStream)) {
                result = stream.toList();
            }

            assertThat(result).hasSize(23);
            assertThat(result).allSatisfy(t -> assertThat(t.getAmount()).isNotEqualByComparingTo(BigDecimal.ZERO));

            Transaction first = result.get(0);
            assertThat(first.getDescription()).isEqualTo("BEST BUY 00005165295 ALPHARETTA GA");
            assertThat(first.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(first.getAmount()).isEqualByComparingTo(new BigDecimal("70.03"));
        }
    }

    /** The older Debit/Credit-column export format (superseded by a single Amount column) still classifies a credit-only row as INCOME rather than TRANSFER_IN, for the same PF-198/PF-829 reasons as {@link ValidCsvTests}. */
    @Nested
    @DisplayName("parse() — legacy Debit/Credit format")
    class LegacyDebitCreditFormatTests {

        @Test
        @DisplayName("should return INCOME for a Credit-only legacy row, not TRANSFER_IN (PF-198, PF-829)")
        void parse_legacyCreditOnlyRow_returnsIncome() {
            String csv = "Trans. Date,Description,Debit,Credit,Category\n" +
                    "6/15/2022,PAYMENT - THANK YOU,,500.00,Payment\n";

            List<Transaction> result;
            try (Stream<Transaction> stream = parser.parse(ACCOUNT_ID, toStream(csv))) {
                result = stream.toList();
            }

            assertThat(result).hasSize(1);
            Transaction t = result.get(0);
            assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        }
    }
}
