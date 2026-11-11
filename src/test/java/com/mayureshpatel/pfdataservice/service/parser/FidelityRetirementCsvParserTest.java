package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies {@code FidelityRetirementCsvParser} against rows copied from a real 401(k) "History for Account" export. */
@DisplayName("FidelityRetirementCsvParser unit tests")
class FidelityRetirementCsvParserTest {

    private static final String PREAMBLE = "\n\n";
    private static final String HEADER = "Date,Investment,Transaction Type,Shares/Unit,Amount ($)\n";
    private static final String CONTRIBUTION_ROW = "12/31/2025,SS TRGT RET 2060 III,Contributions,55.587,676.05\n";
    private static final String CREDIT_ROW = "07/11/2025,SS TRGT RET 2060 III,REVENUE CREDIT,0.035,0.39\n";
    private static final String GAIN_ROW = "03/28/2025,SS TRGT RET 2060 IV,Investment Gain/Loss,0,1693.13\n";
    private static final String EXCHANGE_OUT_ROW = "03/28/2025,SS TRGT RET 2060 IV,Exchange Out,\"-689.848\",\"-12936.03\"\n";
    private static final String EXCHANGE_IN_ROW = "03/28/2025,SS TRGT RET 2060 III,Exchange In,1293.603,12936.03\n";
    private static final String DISCLAIMER = "\n\"The data and information in this spreadsheet is provided to you solely for your use.\"\n"
            + "\"Date downloaded 11/11/2026 10:00 AM\"\n";

    private final FidelityRetirementCsvParser parser = new FidelityRetirementCsvParser();

    private List<Transaction> parse(String csv) {
        return parser.parse(1L, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))).toList();
    }

    private String dateOf(Transaction t) {
        return t.getTransactionDate().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().toString();
    }

    @Test
    @DisplayName("getBankName() should return FIDELITY_RETIREMENT")
    void getBankName_returnsFidelityRetirement() {
        // arrange

        // act
        BankName result = parser.getBankName();

        // assert & verify
        assertThat(result).isEqualTo(BankName.FIDELITY_RETIREMENT);
    }

    @Test
    @DisplayName("Contributions row becomes a positive CONTRIBUTION")
    void parse_contribution() {
        // arrange
        String csv = PREAMBLE + HEADER + CONTRIBUTION_ROW;

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).hasSize(1);
        Transaction t = result.get(0);
        assertThat(t.getType()).isEqualTo(TransactionType.CONTRIBUTION);
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("676.05"));
        assertThat(dateOf(t)).isEqualTo("2025-12-31");
        assertThat(t.getDescription()).contains("SS TRGT RET 2060 III");
    }

    @Test
    @DisplayName("REVENUE CREDIT row becomes EARNINGS")
    void parse_revenueCredit_isEarnings() {
        // arrange
        String csv = PREAMBLE + HEADER + CREDIT_ROW;

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(TransactionType.EARNINGS);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("0.39"));
    }

    @Test
    @DisplayName("Exchange In/Out and Investment Gain/Loss rows are skipped")
    void parse_internalRows_areSkipped() {
        // arrange
        String csv = PREAMBLE + HEADER + GAIN_ROW + EXCHANGE_OUT_ROW + EXCHANGE_IN_ROW;

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Mixed file keeps only the money-in rows and ignores the trailing disclaimer")
    void parse_mixedFileWithDisclaimer() {
        // arrange
        String csv = PREAMBLE + HEADER + CONTRIBUTION_ROW + CREDIT_ROW + GAIN_ROW
                + EXCHANGE_OUT_ROW + EXCHANGE_IN_ROW + DISCLAIMER;

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).extracting(Transaction::getType)
                .containsExactly(TransactionType.CONTRIBUTION, TransactionType.EARNINGS);
    }

    @Test
    @DisplayName("Header on line 1 with no preamble is accepted")
    void parse_headerOnFirstLine() {
        // arrange
        String csv = HEADER + CONTRIBUTION_ROW;

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("CRLF line endings parse the same as LF")
    void parse_crlf() {
        // arrange
        String csv = (PREAMBLE + HEADER + CONTRIBUTION_ROW + CREDIT_ROW + DISCLAIMER).replace("\n", "\r\n");

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Unrecognized transaction type throws naming the value and row, never dropped")
    void parse_unknownType_throws() {
        // arrange
        String csv = PREAMBLE + HEADER + CONTRIBUTION_ROW + "01/02/2026,SS TRGT RET 2060 III,Withdrawal,-1.0,-50.00\n";

        // act & assert & verify
        assertThatThrownBy(() -> parse(csv))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("Withdrawal")
                .hasMessageContaining("row");
    }

    @Test
    @DisplayName("File without the Date header row throws CsvParsingException")
    void parse_noHeader_throws() {
        // arrange
        String csv = PREAMBLE + CONTRIBUTION_ROW;

        // act & assert & verify
        assertThatThrownBy(() -> parse(csv)).isInstanceOf(CsvParsingException.class);
    }

    @Test
    @DisplayName("Bad date throws CsvParsingException")
    void parse_badDate_throws() {
        // arrange
        String csv = PREAMBLE + HEADER + "2025-12-31,SS TRGT RET 2060 III,Contributions,55.587,676.05\n";

        // act & assert & verify
        assertThatThrownBy(() -> parse(csv)).isInstanceOf(CsvParsingException.class);
    }

    @Test
    @DisplayName("Empty input produces no transactions")
    void parse_empty_returnsNothing() {
        // arrange

        // act
        List<Transaction> result = parse("");

        // assert & verify
        assertThat(result).isEmpty();
    }
}
