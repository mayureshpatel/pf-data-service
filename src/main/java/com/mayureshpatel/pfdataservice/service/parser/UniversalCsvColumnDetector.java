package com.mayureshpatel.pfdataservice.service.parser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Fuzzy header-name column detection for {@link UniversalCsvParser} (PF-809: extracted to its
 * own class -- matching header name variants to a column role is a genuinely separate concern
 * from parsing rows once the mapping is known, and splitting it out of the parser itself is what
 * resolved {@code UniversalCsvParser}'s PMD {@code GodClass} flag: a method-level split alone
 * wouldn't have, since moving logic between methods in the same class doesn't change that class's
 * own total weighted complexity or how much foreign data it touches directly).
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class UniversalCsvColumnDetector {

    private static final Pattern DATE_PATTERN = Pattern.compile("^(transaction\\s*date|trans\\s*date|date)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern POST_DATE_PATTERN = Pattern.compile("^(post\\s*date|posted\\s*date)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESC_PATTERN = Pattern.compile("^(description|original\\s*description|memo|payee|merchant)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^(amount|amount\\s*\\(?\\$\\)?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEBIT_PATTERN = Pattern.compile("^(debit|debit\\s*\\(?\\$\\)?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREDIT_PATTERN = Pattern.compile("^(credit|credit\\s*\\(?\\$\\)?)$", Pattern.CASE_INSENSITIVE);

    /**
     * The resolved header names for a parsed file's columns, as identified by {@link #detect}.
     * Any field may be null except {@code dateCol} and {@code descCol}, which {@link #detect}
     * guarantees are set before returning.
     */
    record ColumnMapping(
            String dateCol,
            String postDateCol,
            String descCol,
            String amountCol,
            String debitCol,
            String creditCol
    ) {
    }

    /**
     * Identifies column mappings based on header names.
     *
     * @param headerMap Map of header names to column indices
     * @return ColumnMapping object with identified column names
     */
    static ColumnMapping detect(Map<String, Integer> headerMap) {
        MutableState state = new MutableState();
        for (String col : headerMap.keySet()) {
            matchColumn(col.trim(), col, state);
        }

        // fallback: if "date" not found but "post date" is, use post date as date
        applyPostDateFallback(state);
        validateRequiredColumns(state);

        log.info("Universal Parser Mapped Columns - Date: {}, PostDate: {}, Desc: {}, Amount: {}, Debit: {}, Credit: {}",
                state.dateCol, state.postDateCol, state.descCol, state.amountCol, state.debitCol, state.creditCol);

        return new ColumnMapping(state.dateCol, state.postDateCol, state.descCol, state.amountCol, state.debitCol, state.creditCol);
    }

    /**
     * Tries each pattern in turn against one header column, in the same mutually-exclusive order
     * as before extraction (PF-809): a column already consumed by an earlier match for that same
     * slot doesn't block it from being checked against the remaining patterns on a later column.
     * Split across two equally-sized groups purely to stay under this rule's own method-level
     * complexity threshold -- the two groups together are exactly the original single chain.
     */
    private static void matchColumn(String cleanCol, String col, MutableState state) {
        if (matchDateDescGroup(cleanCol, col, state)) {
            return;
        }
        matchAmountGroup(cleanCol, col, state);
    }

    private static boolean matchDateDescGroup(String cleanCol, String col, MutableState state) {
        if (state.dateCol == null && DATE_PATTERN.matcher(cleanCol).matches()) {
            state.dateCol = col;
            return true;
        }
        if (state.postDateCol == null && POST_DATE_PATTERN.matcher(cleanCol).matches()) {
            state.postDateCol = col;
            return true;
        }
        if (state.descCol == null && DESC_PATTERN.matcher(cleanCol).matches()) {
            state.descCol = col;
            return true;
        }
        return false;
    }

    private static void matchAmountGroup(String cleanCol, String col, MutableState state) {
        if (state.amountCol == null && AMOUNT_PATTERN.matcher(cleanCol).matches()) {
            state.amountCol = col;
        } else if (state.debitCol == null && DEBIT_PATTERN.matcher(cleanCol).matches()) {
            state.debitCol = col;
        } else if (state.creditCol == null && CREDIT_PATTERN.matcher(cleanCol).matches()) {
            state.creditCol = col;
        }
    }

    private static void applyPostDateFallback(MutableState state) {
        if (state.dateCol == null && state.postDateCol != null) {
            state.dateCol = state.postDateCol;
        }
    }

    private static void validateRequiredColumns(MutableState state) {
        if (state.dateCol == null) {
            throw new IllegalArgumentException("Could not find a valid 'Date' column in CSV headers.");
        }
        if (state.descCol == null) {
            throw new IllegalArgumentException("Could not find a valid 'Description' column in CSV headers.");
        }
        // need either amount or (debit and credit)
        if (state.amountCol == null && state.debitCol == null && state.creditCol == null) {
            throw new IllegalArgumentException("Could not find valid 'Amount' or 'Debit/Credit' columns.");
        }
    }

    /** The in-progress, independently-nullable column assignments {@link #matchColumn} fills in as {@link #detect} scans the header row. */
    private static final class MutableState {
        private String dateCol;
        private String postDateCol;
        private String descCol;
        private String amountCol;
        private String debitCol;
        private String creditCol;
    }
}
