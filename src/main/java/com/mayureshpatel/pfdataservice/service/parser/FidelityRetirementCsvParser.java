package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.exception.CsvParsingException;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Parser for Fidelity 401(k) "History for Account" exports:
 * {@code Date, Investment, Transaction Type, Shares/Unit, Amount ($)}. The header is the first row
 * beginning with {@code Date,} (blank lines precede it) and a legal disclaimer follows the data.
 * <p>
 * Mapping: {@code Contributions} is {@code CONTRIBUTION}; {@code REVENUE CREDIT} is {@code EARNINGS}.
 * {@code Exchange In}, {@code Exchange Out} and {@code Investment Gain/Loss} are skipped (fund moves
 * and unrealized valuation, not money in). Any other transaction type throws rather than being
 * dropped, so a withdrawal can never be silently lost or misclassified.
 */
@Component
public class FidelityRetirementCsvParser implements TransactionParser {
    private static final String KIND_CONTRIBUTIONS = "contributions";
    private static final String KIND_REVENUE_CREDIT = "revenue credit";
    private static final Set<String> SKIPPED_TYPES = Set.of("exchange in", "exchange out", "investment gain/loss");

    /**
     * {@inheritDoc}
     */
    @Override
    public BankName getBankName() {
        return BankName.FIDELITY_RETIREMENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Transaction> parse(Long accountId, InputStream inputStream) {
        return FidelityCsvSupport.readRows(inputStream, "Date,", "retirement", this::mapToTransaction).stream();
    }

    /** Maps one data row to a transaction, or null when its type is an explicitly skipped internal type. */
    private Transaction mapToTransaction(CSVRecord csvRecord) {
        String kind = csvRecord.isMapped("Transaction Type") ? csvRecord.get("Transaction Type") : "";
        TransactionType type = resolveType(kind, csvRecord.getRecordNumber());
        if (type == null) {
            return null;
        }
        String investment = csvRecord.isMapped("Investment") ? csvRecord.get("Investment") : "";

        return Transaction.builder()
                .description(investment.isEmpty() ? kind : investment + " - " + kind)
                .amount(FidelityCsvSupport.requirePositive(parseAmount(csvRecord, "Amount ($)"), kind.trim(), csvRecord.getRecordNumber()))
                .transactionDate(FidelityCsvSupport.parseDate(csvRecord.get("Date"), csvRecord.getRecordNumber()))
                .type(type)
                .build();
    }

    /** Returns the mapped type, or null for an explicitly skipped internal type. */
    private TransactionType resolveType(String kind, long recordNumber) {
        String normalized = kind.trim().toLowerCase(Locale.ROOT);
        if (KIND_CONTRIBUTIONS.equals(normalized)) {
            return TransactionType.CONTRIBUTION;
        }
        if (KIND_REVENUE_CREDIT.equals(normalized)) {
            return TransactionType.EARNINGS;
        }
        if (SKIPPED_TYPES.contains(normalized)) {
            return null;
        }
        throw new CsvParsingException("Unsupported Fidelity 401(k) transaction type '" + kind.trim()
                + "' at row " + recordNumber + ".");
    }
}
