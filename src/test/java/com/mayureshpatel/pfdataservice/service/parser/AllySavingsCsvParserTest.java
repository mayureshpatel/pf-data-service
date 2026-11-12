package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies {@code AllySavingsCsvParser}'s mapping of Ally's {@code Date, Time, Amount, Type, Description} rows. */
@DisplayName("AllySavingsCsvParser unit tests")
class AllySavingsCsvParserTest {

    private static final String HEADER = "Date, Time, Amount, Type, Description\n";
    private static final String INTEREST_ROW = "2026-10-08,23:34:12,12.47,Deposit,Interest Paid\n";
    private static final String TRANSFER_ROW =
            "2026-05-15,05:30:01,5000.00,Deposit,Requested transfer from MAYURESH PATEL Ally Bank Transfer\n";

    private final AllySavingsCsvParser parser = new AllySavingsCsvParser();

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    private List<Transaction> parse(String csv) {
        return parser.parse(1L, toStream(csv)).toList();
    }

    private String dateOf(Transaction t) {
        return t.getTransactionDate().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().toString();
    }

    @Test
    @DisplayName("getBankName() should return ALLY")
    void getBankName_returnsAlly() {
        // arrange

        // act
        BankName result = parser.getBankName();

        // assert & verify
        assertThat(result).isEqualTo(BankName.ALLY);
    }

    @Test
    @DisplayName("Interest Paid deposit becomes EARNINGS on its date")
    void parse_interestPaid_isEarnings() {
        // arrange
        String csv = HEADER + INTEREST_ROW;

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).hasSize(1);
        Transaction t = result.get(0);
        assertThat(t.getType()).isEqualTo(TransactionType.EARNINGS);
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("12.47"));
        assertThat(dateOf(t)).isEqualTo("2026-10-08");
    }

    @Test
    @DisplayName("interest paid in different casing is still EARNINGS")
    void parse_interestPaidDifferentCasing_isEarnings() {
        // arrange
        String csv = HEADER + "2026-10-08,23:34:12,12.47,Deposit,INTEREST paid\n";

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).extracting(Transaction::getType).containsExactly(TransactionType.EARNINGS);
    }

    @Test
    @DisplayName("Transfer deposit becomes TRANSFER_IN")
    void parse_transferDeposit_isTransferIn() {
        // arrange
        String csv = HEADER + TRANSFER_ROW;

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).hasSize(1);
        Transaction t = result.get(0);
        assertThat(t.getType()).isEqualTo(TransactionType.TRANSFER_IN);
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(dateOf(t)).isEqualTo("2026-05-15");
    }

    @Test
    @DisplayName("Other deposit becomes INCOME")
    void parse_otherDeposit_isIncome() {
        // arrange
        String csv = HEADER + "2026-06-01,10:00:00,100.00,Deposit,Mobile check\n";

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).extracting(Transaction::getType).containsExactly(TransactionType.INCOME);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Withdrawal becomes TRANSFER_OUT")
    void parse_withdrawal_isTransferOut() {
        // arrange
        String csv = HEADER + "2026-06-02,10:00:00,40.00,Withdrawal,Move money out\n";

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).extracting(Transaction::getType).containsExactly(TransactionType.TRANSFER_OUT);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("Withdrawal whose description mentions Transfer is TRANSFER_OUT, not TRANSFER_IN")
    void parse_withdrawalWithTransferDescription_isTransferOut() {
        // arrange
        String csv = HEADER + "2026-06-02,10:00:00,40.00,Withdrawal,Requested transfer to Checking Ally Bank Transfer\n";

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).extracting(Transaction::getType).containsExactly(TransactionType.TRANSFER_OUT);
    }

    @Test
    @DisplayName("Unknown Type throws CsvParsingException naming the type and row")
    void parse_unknownType_throws() {
        // arrange
        String csv = HEADER + INTEREST_ROW + "2026-06-02,10:00:00,5.00,Fee,Monthly fee\n";

        // act & assert & verify
        assertThatThrownBy(() -> parse(csv))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Fee")
                .hasMessageContaining("row");
    }

    @Test
    @DisplayName("Missing Type value throws CsvParsingException")
    void parse_missingType_throws() {
        // arrange
        String csv = HEADER + "2026-06-02,10:00:00,5.00,,Mystery\n";

        // act & assert & verify
        assertThatThrownBy(() -> parse(csv))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Unsupported Ally transaction type");
    }

    @Test
    @DisplayName("Header whitespace is tolerated and trailing empty lines are ignored")
    void parse_headerWhitespaceAndTrailingBlankLines() {
        // arrange
        String csv = HEADER + INTEREST_ROW + "\n\n";

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Header-only and empty input produce no transactions")
    void parse_headerOnlyAndEmpty_returnsNothing() {
        // arrange
        String headerOnly = HEADER;
        String empty = "";

        // act
        List<Transaction> fromHeaderOnly = parse(headerOnly);
        List<Transaction> fromEmpty = parse(empty);

        // assert & verify
        assertThat(fromHeaderOnly).isEmpty();
        assertThat(fromEmpty).isEmpty();
    }

    @Test
    @DisplayName("Quoted description containing a comma is kept whole")
    void parse_quotedDescriptionWithComma() {
        // arrange
        String csv = HEADER + "2026-06-01,10:00:00,100.00,Deposit,\"Payroll, ACME Corp\"\n";

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("Payroll, ACME Corp");
    }

    @Test
    @DisplayName("CRLF line endings parse the same as LF")
    void parse_crlfLineEndings() {
        // arrange
        String csv = (HEADER + INTEREST_ROW + TRANSFER_ROW).replace("\n", "\r\n");

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).extracting(Transaction::getType)
                .containsExactly(TransactionType.EARNINGS, TransactionType.TRANSFER_IN);
    }
}
