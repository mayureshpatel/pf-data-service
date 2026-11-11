package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.repository.file_import_history.FileImportHistoryRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.ImportDuplicateDetector.DedupeResult;
import com.mayureshpatel.pfdataservice.service.parser.FidelityBrokerageCsvParser;
import com.mayureshpatel.pfdataservice.service.parser.FidelityRetirementCsvParser;
import com.mayureshpatel.pfdataservice.service.parser.TransactionParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Importing the same Fidelity file twice must flag every row of the second pass as a duplicate. */
@DisplayName("Fidelity import duplicate detection")
class FidelityImportDuplicateTest {

    private static final String RETIREMENT_CSV = "\n\nDate,Investment,Transaction Type,Shares/Unit,Amount ($)\n"
            + "12/31/2025,SS TRGT RET 2060 III,Contributions,55.587,676.05\n"
            + "07/11/2025,SS TRGT RET 2060 III,REVENUE CREDIT,0.035,0.39\n"
            + "03/28/2025,SS TRGT RET 2060 III,Exchange In,1293.603,12936.03\n";
    private static final String BROKERAGE_CSV = "\n\nRun Date,Action,Symbol,Description,Type,Price ($),Quantity,Commission ($),"
            + "Fees ($),Accrued Interest ($),Amount ($),Cash Balance ($),Settlement Date\n"
            + "10/19/2026,CASH CONTRIBUTION CURRENT YEAR (Cash),\"\",No Description,Cash,\"\",0,\"\",\"\",\"\",500,500.02,\"\"\n"
            + "10/30/2026,DIVIDEND RECEIVED FIDELITY GOVERNMENT MONEY MARKET (SPAXX) (Cash),SPAXX,"
            + "FIDELITY GOVERNMENT MONEY MARKET,Cash,\"\",0,\"\",\"\",\"\",0.82,500.84,\"\"\n";

    private static final String SAME_AMOUNT_RETIREMENT_CSV = "\n\nDate,Investment,Transaction Type,Shares/Unit,Amount ($)\n"
            + "11/30/2025,SS TRGT RET 2060 III,Contributions,55.587,676.05\n"
            + "12/31/2025,SS TRGT RET 2060 III,Contributions,55.587,676.05\n";
    private static final String SAME_AMOUNT_BROKERAGE_CSV = BROKERAGE_CSV.split("10/30/2026")[0]
            + "11/19/2026,CASH CONTRIBUTION CURRENT YEAR (Cash),\"\",No Description,Cash,\"\",0,\"\",\"\",\"\",500,1000.02,\"\"\n";

    private final ImportDuplicateDetector detector = new ImportDuplicateDetector(
            Mockito.mock(TransactionRepository.class), Mockito.mock(FileImportHistoryRepository.class));
    private final Account account = Account.builder().id(10L).userId(1L).build();

    private List<TransactionDto> parseToDtos(TransactionParser parser, String csv) {
        return parser.parse(10L, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)))
                .map(this::toDto)
                .toList();
    }

    private TransactionDto toDto(Transaction t) {
        return TransactionDto.builder().date(t.getTransactionDate()).description(t.getDescription())
                .amount(t.getAmount()).type(t.getType()).build();
    }

    private Set<String> signaturesOf(List<TransactionDto> rows) {
        Set<String> signatures = new HashSet<>();
        rows.forEach(r -> signatures.add(
                ImportDuplicateDetector.buildSignature(r.date(), r.amount(), r.description(), r.type())));
        return signatures;
    }

    private void assertSecondPassIsAllDuplicates(TransactionParser parser, String csv, int expectedRows) {
        // arrange
        List<TransactionDto> firstPass = parseToDtos(parser, csv);
        DedupeResult first = detector.deduplicate(firstPass, Set.of(), account, Map.of());
        Set<String> saved = signaturesOf(firstPass);

        // act
        DedupeResult second = detector.deduplicate(parseToDtos(parser, csv), saved, account, Map.of());

        // assert & verify
        assertThat(first.uniqueTransactions()).hasSize(expectedRows);
        assertThat(first.duplicateCount()).isZero();
        assertThat(second.uniqueTransactions()).isEmpty();
        assertThat(second.duplicateCount()).isEqualTo(expectedRows);
    }

    @Test
    @DisplayName("retirement file imported twice: second pass is all duplicates")
    void retirement_secondImportIsAllDuplicates() {
        assertSecondPassIsAllDuplicates(new FidelityRetirementCsvParser(), RETIREMENT_CSV, 2);
    }

    @Test
    @DisplayName("brokerage file imported twice: second pass is all duplicates")
    void brokerage_secondImportIsAllDuplicates() {
        assertSecondPassIsAllDuplicates(new FidelityBrokerageCsvParser(), BROKERAGE_CSV, 2);
    }

    @Test
    @DisplayName("retirement: same amount and description on different dates are both unique, then both duplicates")
    void retirement_sameAmountDifferentDates() {
        assertSecondPassIsAllDuplicates(new FidelityRetirementCsvParser(), SAME_AMOUNT_RETIREMENT_CSV, 2);
    }

    @Test
    @DisplayName("brokerage: same amount and description on different dates are both unique, then both duplicates")
    void brokerage_sameAmountDifferentDates() {
        assertSecondPassIsAllDuplicates(new FidelityBrokerageCsvParser(), SAME_AMOUNT_BROKERAGE_CSV, 2);
    }
}
