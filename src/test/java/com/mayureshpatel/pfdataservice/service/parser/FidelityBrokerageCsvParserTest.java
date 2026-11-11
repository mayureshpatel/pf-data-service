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

/** Verifies {@code FidelityBrokerageCsvParser} against rows copied from a real Roth IRA brokerage export. */
@DisplayName("FidelityBrokerageCsvParser unit tests")
class FidelityBrokerageCsvParserTest {

    private static final String PREAMBLE = "\n\n";
    private static final String HEADER = "Run Date,Action,Symbol,Description,Type,Price ($),Quantity,Commission ($),"
            + "Fees ($),Accrued Interest ($),Amount ($),Cash Balance ($),Settlement Date\n";
    private static final String CONTRIBUTION_ROW =
            "10/19/2026,CASH CONTRIBUTION CURRENT YEAR (Cash),\"\",No Description,Cash,\"\",0,\"\",\"\",\"\",500,500.02,\"\"\n";
    private static final String DIVIDEND_ROW =
            "10/30/2026,DIVIDEND RECEIVED FIDELITY GOVERNMENT MONEY MARKET (SPAXX) (Cash),SPAXX,"
                    + "FIDELITY GOVERNMENT MONEY MARKET,Cash,\"\",0,\"\",\"\",\"\",0.82,500.84,\"\"\n";
    private static final String BOUGHT_ROW =
            "10/20/2026,YOU BOUGHT VANGUARD INDEX FDS S&P 500 ETF (VOO) (Cash),VOO,VANGUARD S&P 500 ETF,Cash,"
                    + "500.1,0.9,\"\",\"\",\"\",-450.09,50.00,10/21/2026\n";
    private static final String PROSPECTUS_ROW =
            "10/20/2026,YOU BOUGHT PROSPECTUS UNDER SEPARATE COVER FIDELITY 500 INDEX FUND (FXAIX) (Cash),FXAIX,"
                    + "FIDELITY 500 INDEX FUND,Cash,230.5,1,\"\",\"\",\"\",-230.5,0,10/21/2026\n";
    private static final String REINVEST_ROW =
            "10/30/2026,REINVESTMENT FIDELITY GOVERNMENT MONEY MARKET (SPAXX) (Cash),SPAXX,"
                    + "FIDELITY GOVERNMENT MONEY MARKET,Cash,1,0.82,\"\",\"\",\"\",-0.82,0,\"\"\n";
    private static final String DISCLAIMER = "\n\"The data and information in this spreadsheet is provided to you solely for your use.\"\n"
            + "\"Date downloaded 11/11/2026 10:00 AM\"\n";

    private final FidelityBrokerageCsvParser parser = new FidelityBrokerageCsvParser();

    private List<Transaction> parse(String csv) {
        return parser.parse(1L, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))).toList();
    }

    private String dateOf(Transaction t) {
        return t.getTransactionDate().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().toString();
    }

    @Test
    @DisplayName("getBankName() should return FIDELITY_BROKERAGE")
    void getBankName_returnsFidelityBrokerage() {
        // arrange

        // act
        BankName result = parser.getBankName();

        // assert & verify
        assertThat(result).isEqualTo(BankName.FIDELITY_BROKERAGE);
    }

    @Test
    @DisplayName("CASH CONTRIBUTION becomes CONTRIBUTION 500.00")
    void parse_cashContribution() {
        // arrange
        String csv = PREAMBLE + HEADER + CONTRIBUTION_ROW;

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).hasSize(1);
        Transaction t = result.get(0);
        assertThat(t.getType()).isEqualTo(TransactionType.CONTRIBUTION);
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(dateOf(t)).isEqualTo("2026-10-19");
        assertThat(t.getDescription()).startsWith("CASH CONTRIBUTION");
    }

    @Test
    @DisplayName("DIVIDEND RECEIVED becomes EARNINGS 0.82")
    void parse_dividend_isEarnings() {
        // arrange
        String csv = PREAMBLE + HEADER + DIVIDEND_ROW;

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(TransactionType.EARNINGS);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("0.82"));
        assertThat(dateOf(result.get(0))).isEqualTo("2026-10-30");
    }

    @Test
    @DisplayName("YOU BOUGHT (incl. prospectus) and REINVESTMENT rows are skipped")
    void parse_internalRows_areSkipped() {
        // arrange
        String csv = PREAMBLE + HEADER + BOUGHT_ROW + PROSPECTUS_ROW + REINVEST_ROW;

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Mixed file keeps only contribution and dividend and ignores the disclaimer")
    void parse_mixedFileWithDisclaimer() {
        // arrange
        String csv = PREAMBLE + HEADER + CONTRIBUTION_ROW + BOUGHT_ROW + DIVIDEND_ROW + REINVEST_ROW + DISCLAIMER;

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).extracting(Transaction::getType)
                .containsExactly(TransactionType.CONTRIBUTION, TransactionType.EARNINGS);
    }

    @Test
    @DisplayName("Header on line 1 and CRLF endings are accepted")
    void parse_noPreambleAndCrlf() {
        // arrange
        String csv = (HEADER + CONTRIBUTION_ROW + DISCLAIMER).replace("\n", "\r\n");

        // act
        List<Transaction> result = parse(csv);

        // assert & verify
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Unrecognized action (e.g. a withdrawal) throws naming the action and row")
    void parse_unknownAction_throws() {
        // arrange
        String csv = PREAMBLE + HEADER + CONTRIBUTION_ROW
                + "11/02/2026,DISTRIBUTION NORMAL (Cash),\"\",No Description,Cash,\"\",0,\"\",\"\",\"\",-100,400.02,\"\"\n";

        // act & assert & verify
        assertThatThrownBy(() -> parse(csv))
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("DISTRIBUTION NORMAL")
                .hasMessageContaining("row");
    }

    @Test
    @DisplayName("File without the Run Date header throws CsvParsingException")
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
        String csv = PREAMBLE + HEADER + CONTRIBUTION_ROW.replace("10/19/2026", "2026-10-19");

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
