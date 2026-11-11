package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Parser for Fidelity brokerage (Roth IRA) activity exports whose header begins
 * {@code Run Date, Action, Symbol, Description, ...}. The header is the first row beginning with
 * {@code Run Date,} (blank lines precede it) and a legal disclaimer follows the data. The
 * {@code Action} column is a long descriptive string, so rows are matched by prefix.
 * <p>
 * Mapping: {@code CASH CONTRIBUTION...} is {@code CONTRIBUTION}; {@code DIVIDEND RECEIVED...} is
 * {@code EARNINGS}. {@code YOU BOUGHT...} (including the prospectus rows) and {@code REINVESTMENT...}
 * are skipped as internal movement, not money in. Any other action throws rather than being
 * dropped, so a withdrawal can never be silently lost or misclassified.
 */
@Component
public class FidelityBrokerageCsvParser implements TransactionParser {
    private static final String CONTRIBUTION_PREFIX = "CASH CONTRIBUTION";
    private static final String DIVIDEND_PREFIX = "DIVIDEND RECEIVED";
    private static final String BOUGHT_PREFIX = "YOU BOUGHT";
    private static final String REINVESTMENT_PREFIX = "REINVESTMENT";

    /**
     * {@inheritDoc}
     */
    @Override
    public BankName getBankName() {
        return BankName.FIDELITY_BROKERAGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        return FidelityCsvSupport.readRows(inputStream, "Run Date,", "brokerage", this::mapToTransaction).stream();
    }

    private Transaction mapToTransaction(CSVRecord csvRecord) {
        String action = csvRecord.isMapped("Action") ? csvRecord.get("Action") : "";
        TransactionType type = resolveType(action, csvRecord.getRecordNumber());
        if (type == null) {
            return null;
        }

        return Transaction.builder()
                .description(action.trim())
                .amount(parseAmount(csvRecord, "Amount ($)").abs())
                .transactionDate(FidelityCsvSupport.parseDate(csvRecord.get("Run Date"), csvRecord.getRecordNumber()))
                .type(type)
                .build();
    }

    /** Returns the mapped type, or null for an explicitly skipped internal action. */
    private TransactionType resolveType(String action, long recordNumber) {
        String normalized = action.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith(CONTRIBUTION_PREFIX)) {
            return TransactionType.CONTRIBUTION;
        }
        if (normalized.startsWith(DIVIDEND_PREFIX)) {
            return TransactionType.EARNINGS;
        }
        if (normalized.startsWith(BOUGHT_PREFIX) || normalized.startsWith(REINVESTMENT_PREFIX)) {
            return null;
        }
        throw new CsvParsingException("Unsupported Fidelity brokerage action '" + action.trim()
                + "' at row " + recordNumber + ".");
    }
}
