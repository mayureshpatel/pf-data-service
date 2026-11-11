package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies {@code AllySavingsCsvParser}'s mapping of Ally's {@code Date, Time, Amount, Type, Description} rows. */
@DisplayName("AllySavingsCsvParser unit tests")
class AllySavingsCsvParserTest {

    private static final String HEADER = "Date, Time, Amount, Type, Description\n";
    private final AllySavingsCsvParser parser = new AllySavingsCsvParser();

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    private List<Transaction> parse(String csv) {
        return parser.parse(1L, toStream(csv)).toList();
    }

    @Test
    @DisplayName("getBankName() should return ALLY")
    void getBankName_returnsAlly() {
        assertThat(parser.getBankName()).isEqualTo(BankName.ALLY);
    }

    @Test
    @DisplayName("Interest Paid deposit becomes EARNINGS on its date")
    void parse_interestPaid_isEarnings() {
        List<Transaction> result = parse(HEADER + "2026-10-08,23:34:12,12.47,Deposit,Interest Paid\n");

        assertThat(result).hasSize(1);
        Transaction t = result.get(0);
        assertThat(t.getType()).isEqualTo(TransactionType.EARNINGS);
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("12.47"));
        assertThat(t.getTransactionDate().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().toString())
                .isEqualTo("2026-10-08");
    }

    @Test
    @DisplayName("Transfer deposit becomes TRANSFER_IN")
    void parse_transferDeposit_isTransferIn() {
        List<Transaction> result = parse(HEADER
                + "2026-05-15,05:30:01,5000.00,Deposit,Requested transfer from Checking Ally Bank Transfer\n");

        assertThat(result).hasSize(1);
        Transaction t = result.get(0);
        assertThat(t.getType()).isEqualTo(TransactionType.TRANSFER_IN);
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(t.getTransactionDate().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().toString())
                .isEqualTo("2026-05-15");
    }

    @Test
    @DisplayName("Other deposit becomes INCOME and withdrawal becomes TRANSFER_OUT")
    void parse_otherDepositAndWithdrawal() {
        List<Transaction> result = parse(HEADER
                + "2026-06-01,10:00:00,100.00,Deposit,Mobile check\n"
                + "2026-06-02,10:00:00,40.00,Withdrawal,Move money out\n");

        assertThat(result).extracting(Transaction::getType)
                .containsExactly(TransactionType.INCOME, TransactionType.TRANSFER_OUT);
        assertThat(result.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("Header whitespace is tolerated and trailing empty lines are ignored")
    void parse_headerWhitespaceAndTrailingBlankLines() {
        List<Transaction> result = parse(HEADER + "2026-10-08,23:34:12,12.47,Deposit,Interest Paid\n\n\n");

        assertThat(result).hasSize(1);
    }
}
