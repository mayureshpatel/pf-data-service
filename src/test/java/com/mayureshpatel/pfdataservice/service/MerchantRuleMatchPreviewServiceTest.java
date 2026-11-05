package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchPreviewDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchPreviewRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchRowDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleRefDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleVerdict;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.RuleMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link MerchantRuleMatchPreviewService}'s PF-905 verdicts, counts, row cap and
 * ordering, using mocked repositories and the real {@link RuleMatcher} so matching semantics are
 * the import ones.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantRuleMatchPreviewService Unit Tests")
class MerchantRuleMatchPreviewServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long EDITED_RULE_ID = 7L;
    private static final Long OTHER_RULE_ID = 8L;
    private static final Long TARGET_ID = 10L;
    private static final String KROGER = "KROGER";
    private static final String KROGER_STORE = "KROGER #123";
    private static final String FUEL = "FUEL";
    private static final String SHELL = "SHELL";
    private static final String KROGER_NAME = "Kroger";
    private static final String FUEL_SUFFIX = " FUEL";
    private static final String AMOUNT = "-25.00";
    private static final OffsetDateTime BASE_DATE = OffsetDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final Merchant OWNED = Merchant.builder().id(60L).name("Owned Store").build();

    @Mock
    private MerchantRuleRepository merchantRuleRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private MerchantService merchantService;

    private MerchantRuleMatchPreviewService service;

    @BeforeEach
    void setUp() {
        service = new MerchantRuleMatchPreviewService(
                merchantRuleRepository, transactionRepository, merchantService, new RuleMatcher());
    }

    private static Transaction tx(long id, String description, String amount, int day, Merchant merchant) {
        return Transaction.builder()
                .id(id)
                .description(description)
                .amount(new BigDecimal(amount))
                .transactionDate(BASE_DATE.plusDays(day))
                .account(Account.builder().id(3L).name("Checking").build())
                .merchant(merchant)
                .build();
    }

    private static Transaction bare(long id, String description, int day) {
        return tx(id, description, AMOUNT, day, null);
    }

    private static MerchantRule rule(Long id, int priority, String... keywords) {
        return MerchantRule.builder()
                .id(id)
                .keywords(List.of(keywords))
                .matchType(MatchType.OR)
                .priority(priority)
                .merchant(Merchant.builder().id(50L).name(KROGER_NAME).build())
                .build();
    }

    private static MerchantRuleMatchPreviewRequest request(Long ruleId, Integer priority, String... keywords) {
        return new MerchantRuleMatchPreviewRequest(ruleId, List.of(keywords), MatchType.OR, priority, null, null, null);
    }

    private void givenRules(MerchantRule... rules) {
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rules));
    }

    private void givenCandidates(List<Transaction> candidates) {
        when(transactionRepository.findCandidatesByDescriptionKeywords(eq(USER_ID), anyList())).thenReturn(candidates);
    }

    private void givenLinks(Map<String, Long> links) {
        when(merchantService.findMatchingMerchantIds(eq(USER_ID), anyList())).thenReturn(links);
    }

    @Test
    @DisplayName("blank or empty keywords return an empty preview without reading any transaction or link")
    void preview_blankKeywords_returnsEmptyAndNeverQueriesTransactions() {
        // arrange
        givenRules();
        MerchantRuleMatchPreviewRequest blank = request(null, 0, "  ", "");
        MerchantRuleMatchPreviewRequest empty = new MerchantRuleMatchPreviewRequest(null, List.of(), null, null, null, null, null);
        MerchantRuleMatchPreviewRequest nullKeywords = new MerchantRuleMatchPreviewRequest(null, null, null, null, null, null, null);

        // act
        List<MerchantRuleMatchPreviewDto> results = List.of(
                service.preview(USER_ID, blank),
                service.preview(USER_ID, empty),
                service.preview(USER_ID, nullKeywords));

        // assert & verify
        assertThat(results).allSatisfy(result -> {
            assertThat(result.totalMatches()).isZero();
            assertThat(result.willAssignCount()).isZero();
            assertThat(result.losesToOtherRuleCount()).isZero();
            assertThat(result.alreadyHasMerchantCount()).isZero();
            assertThat(result.linkedCount()).isZero();
            assertThat(result.truncated()).isFalse();
            assertThat(result.rows()).isEmpty();
        });
        verifyNoInteractions(transactionRepository, merchantService);
    }

    @Test
    @DisplayName("a merchant-less, unlinked transaction only the draft matches is will-assign; rows the draft does not match are dropped")
    void preview_merchantlessMatchedOnlyByDraft_willAssign() {
        // arrange
        givenRules(rule(OTHER_RULE_ID, 5, SHELL));
        givenCandidates(List.of(bare(1L, KROGER_STORE, 1), bare(2L, "SHELL OIL 123", 2)));
        givenLinks(Map.of());

        // act
        MerchantRuleMatchPreviewDto result = service.preview(USER_ID, request(null, 0, "kroger"));

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(1);
        assertThat(result.willAssignCount()).isEqualTo(1);
        assertThat(result.losesToOtherRuleCount()).isZero();
        assertThat(result.alreadyHasMerchantCount()).isZero();
        assertThat(result.linkedCount()).isZero();
        assertThat(result.truncated()).isFalse();
        assertThat(result.rows()).hasSize(1);
        MerchantRuleMatchRowDto row = result.rows().get(0);
        assertThat(row.transactionId()).isEqualTo(1L);
        assertThat(row.description()).isEqualTo(KROGER_STORE);
        assertThat(row.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(row.date()).isEqualTo(BASE_DATE.plusDays(1));
        assertThat(row.accountName()).isEqualTo("Checking");
        assertThat(row.currentMerchant()).isNull();
        assertThat(row.verdict()).isEqualTo(MerchantRuleVerdict.WILL_ASSIGN);
        assertThat(row.otherRules()).isEmpty();
        assertThat(row.winningRule()).isNull();
    }

    @Test
    @DisplayName("a higher-priority saved rule that matches first wins and is named, and the row lists every other matching rule in order")
    void preview_higherPrioritySavedRule_losesAndNamesWinner() {
        // arrange
        givenRules(rule(OTHER_RULE_ID, 10, KROGER), rule(9L, 1, "STORE"));
        givenCandidates(List.of(bare(1L, "KROGER STORE #123", 1)));
        givenLinks(Map.of());

        // act
        MerchantRuleMatchPreviewDto result = service.preview(USER_ID, request(null, 5, "kroger"));

        // assert & verify
        assertThat(result.willAssignCount()).isZero();
        assertThat(result.losesToOtherRuleCount()).isEqualTo(1);
        MerchantRuleMatchRowDto row = result.rows().get(0);
        assertThat(row.verdict()).isEqualTo(MerchantRuleVerdict.LOSES_TO_OTHER_RULE);
        assertThat(row.winningRule()).isEqualTo(new MerchantRuleRefDto(OTHER_RULE_ID, List.of(KROGER), KROGER_NAME, 10));
        assertThat(row.otherRules()).containsExactly(
                new MerchantRuleRefDto(OTHER_RULE_ID, List.of(KROGER), KROGER_NAME, 10),
                new MerchantRuleRefDto(9L, List.of("STORE"), KROGER_NAME, 1));
    }

    @Test
    @DisplayName("at equal priority and specificity a new draft ranks after the saved rule (draft id sorts last)")
    void preview_equalPriorityAndSpecificity_newDraftLoses() {
        // arrange
        givenRules(rule(OTHER_RULE_ID, 0, KROGER));
        givenCandidates(List.of(bare(1L, KROGER_STORE, 1)));
        givenLinks(Map.of());

        // act
        MerchantRuleMatchPreviewDto result = service.preview(USER_ID, request(null, 0, "kroger"));

        // assert & verify
        assertThat(result.losesToOtherRuleCount()).isEqualTo(1);
        assertThat(result.rows().get(0).winningRule().id()).isEqualTo(OTHER_RULE_ID);
    }

    @Test
    @DisplayName("a transaction that already has a merchant is already-has-merchant, never will-assign, and is not looked up for links")
    void preview_transactionWithMerchant_alreadyHasMerchant() {
        // arrange
        givenRules(rule(OTHER_RULE_ID, 0, FUEL));
        givenCandidates(List.of(tx(1L, KROGER_STORE + FUEL_SUFFIX, AMOUNT, 1, OWNED)));
        givenLinks(Map.of());

        // act
        MerchantRuleMatchPreviewDto result = service.preview(USER_ID, request(null, 99, KROGER));

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(1);
        assertThat(result.alreadyHasMerchantCount()).isEqualTo(1);
        assertThat(result.willAssignCount()).isZero();
        assertThat(result.losesToOtherRuleCount()).isZero();
        MerchantRuleMatchRowDto row = result.rows().get(0);
        assertThat(row.verdict()).isEqualTo(MerchantRuleVerdict.ALREADY_HAS_MERCHANT);
        assertThat(row.currentMerchant().id()).isEqualTo(60L);
        assertThat(row.currentMerchant().name()).isEqualTo("Owned Store");
        assertThat(row.otherRules()).extracting(MerchantRuleRefDto::id).containsExactly(OTHER_RULE_ID);
        verify(merchantService).findMatchingMerchantIds(USER_ID, List.of());
    }

    @Test
    @DisplayName("a merchant-less transaction with an exact description link is linked, even when it would otherwise win or lose")
    void preview_linkedDescription_linkedBeatsRules() {
        // arrange
        givenRules(rule(OTHER_RULE_ID, 10, KROGER));
        givenCandidates(List.of(bare(1L, KROGER_STORE, 1), bare(2L, "KROGER #999", 2)));
        givenLinks(Map.of(KROGER_STORE, 60L));

        // act
        MerchantRuleMatchPreviewDto result = service.preview(USER_ID, request(null, 0, KROGER));

        // assert & verify
        assertThat(result.linkedCount()).isEqualTo(1);
        assertThat(result.losesToOtherRuleCount()).isEqualTo(1);
        assertThat(result.rows()).extracting(MerchantRuleMatchRowDto::transactionId, MerchantRuleMatchRowDto::verdict)
                .containsExactly(
                        tuple(2L, MerchantRuleVerdict.LOSES_TO_OTHER_RULE),
                        tuple(1L, MerchantRuleVerdict.LINKED));
        assertThat(result.rows().get(1).winningRule()).isNull();
    }

    @Test
    @DisplayName("links are looked up once, in one batch of the distinct merchant-less matched descriptions only")
    void preview_linkLookup_isOneBatchOfDistinctMerchantlessDescriptions() {
        // arrange
        givenRules();
        givenCandidates(List.of(
                bare(1L, KROGER_STORE, 1),
                bare(2L, KROGER_STORE, 2),
                tx(3L, "KROGER #2", AMOUNT, 3, OWNED),
                bare(4L, SHELL, 4)));
        givenLinks(Map.of());

        // act
        service.preview(USER_ID, request(null, 0, KROGER));

        // assert & verify
        verify(merchantService).findMatchingMerchantIds(USER_ID, List.of(KROGER_STORE));
    }

    @Test
    @DisplayName("editing a rule replaces its saved version in the ordering, so the rule is never compared with itself")
    void preview_editingRule_replacesSavedVersion() {
        // arrange -- the saved version has the same priority and keyword, so if it stayed in the
        // ordering it would tie with the draft; it also matches a row the edited draft no longer does
        givenRules(rule(EDITED_RULE_ID, 0, KROGER, "OLDKEYWORD"));
        givenCandidates(List.of(bare(1L, KROGER_STORE, 1), bare(2L, "OLDKEYWORD STORE", 2)));
        givenLinks(Map.of());

        // act
        MerchantRuleMatchPreviewDto result = service.preview(USER_ID, request(EDITED_RULE_ID, 0, KROGER));

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(1);
        assertThat(result.rows()).singleElement().satisfies(row -> {
            assertThat(row.transactionId()).isEqualTo(1L);
            assertThat(row.verdict()).isEqualTo(MerchantRuleVerdict.WILL_ASSIGN);
            assertThat(row.otherRules()).isEmpty();
        });
    }

    @Test
    @DisplayName("changing an edited rule's priority flips its verdict against another rule")
    void preview_editingRule_priorityChangeFlipsVerdict() {
        // arrange
        when(merchantRuleRepository.findByUserId(USER_ID))
                .thenReturn(List.of(rule(OTHER_RULE_ID, 5, KROGER), rule(EDITED_RULE_ID, 1, KROGER)));
        givenCandidates(List.of(bare(1L, KROGER_STORE, 1)));
        givenLinks(Map.of());

        // act
        MerchantRuleMatchPreviewDto unchanged = service.preview(USER_ID, request(EDITED_RULE_ID, 1, KROGER));
        MerchantRuleMatchPreviewDto raised = service.preview(USER_ID, request(EDITED_RULE_ID, 9, KROGER));

        // assert & verify
        assertThat(unchanged.rows().get(0).verdict()).isEqualTo(MerchantRuleVerdict.LOSES_TO_OTHER_RULE);
        assertThat(unchanged.rows().get(0).winningRule().id()).isEqualTo(OTHER_RULE_ID);
        assertThat(raised.rows().get(0).verdict()).isEqualTo(MerchantRuleVerdict.WILL_ASSIGN);
        assertThat(raised.rows().get(0).otherRules()).extracting(MerchantRuleRefDto::id).containsExactly(OTHER_RULE_ID);
    }

    @Test
    @DisplayName("160 matches give 50 rows and full counts, will-assign first then losing, newest first within each")
    void preview_over50Matches_truncatedRowsAndFullCounts() {
        // arrange -- 20 will-assign, 60 losing (also matched by a higher-priority rule), 40 with a
        // merchant and 40 linked, shuffled so input order cannot explain the output order
        List<Transaction> candidates = new ArrayList<>();
        long id = 1;
        for (int i = 0; i < 20; i++) {
            candidates.add(bare(id++, "KROGER " + i, i * 6));
        }
        for (int i = 0; i < 60; i++) {
            candidates.add(bare(id++, "KROGER PRIME " + i, i * 2 + 1));
        }
        for (int i = 0; i < 40; i++) {
            candidates.add(tx(id++, "KROGER OWNED " + i, "-10.00", i * 3 + 2, OWNED));
        }
        Map<String, Long> links = new java.util.HashMap<>();
        for (int i = 0; i < 40; i++) {
            String description = "KROGER LINKED " + i;
            candidates.add(bare(id++, description, i * 3));
            links.put(description, 60L);
        }
        Collections.shuffle(candidates, new Random(42));
        givenRules(rule(OTHER_RULE_ID, 10, "PRIME"));
        givenCandidates(candidates);
        givenLinks(links);

        // act
        MerchantRuleMatchPreviewDto result = service.preview(USER_ID, request(null, 0, KROGER));

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(160);
        assertThat(result.willAssignCount()).isEqualTo(20);
        assertThat(result.losesToOtherRuleCount()).isEqualTo(60);
        assertThat(result.alreadyHasMerchantCount()).isEqualTo(40);
        assertThat(result.linkedCount()).isEqualTo(40);
        assertThat(result.truncated()).isTrue();
        assertThat(result.rows()).hasSize(MerchantRuleMatchPreviewService.MAX_ROWS);
        List<MerchantRuleMatchRowDto> will = result.rows().subList(0, 20);
        List<MerchantRuleMatchRowDto> loses = result.rows().subList(20, 50);
        assertThat(will).allMatch(row -> row.verdict() == MerchantRuleVerdict.WILL_ASSIGN);
        assertThat(loses).allMatch(row -> row.verdict() == MerchantRuleVerdict.LOSES_TO_OTHER_RULE);
        assertThat(will).isSortedAccordingTo(Comparator.comparing(MerchantRuleMatchRowDto::date).reversed());
        assertThat(loses).isSortedAccordingTo(Comparator.comparing(MerchantRuleMatchRowDto::date).reversed());
        assertThat(loses.get(0).date()).isEqualTo(BASE_DATE.plusDays(119));
        assertThat(loses.get(29).date()).isEqualTo(BASE_DATE.plusDays(61));
    }

    @Test
    @DisplayName("rows are ordered will-assign, losing, has-merchant, linked, newest first within each")
    void preview_rowsOrderedByVerdictThenNewestFirst() {
        // arrange
        givenRules(rule(OTHER_RULE_ID, 10, "PRIME"));
        givenCandidates(List.of(
                tx(1L, KROGER, "-1.00", 9, OWNED),
                bare(2L, "KROGER PRIME", 8),
                bare(3L, KROGER, 1),
                bare(4L, "KROGER PRIME", 2),
                tx(5L, KROGER, "-1.00", 3, OWNED),
                bare(6L, "KROGER LINK", 5),
                bare(7L, "KROGER LINK", 6)));
        givenLinks(Map.of("KROGER LINK", 60L));

        // act
        MerchantRuleMatchPreviewDto result = service.preview(USER_ID, request(null, 0, KROGER));

        // assert & verify
        assertThat(result.rows()).extracting(MerchantRuleMatchRowDto::transactionId)
                .containsExactly(3L, 2L, 4L, 1L, 5L, 7L, 6L);
    }

    @Test
    @DisplayName("an AND draft with an amount range matches through the shared RuleMatcher (absolute amount, inclusive bounds, case-insensitive)")
    void preview_andRuleAndAmountRange_useRuleMatcher() {
        // arrange
        givenRules();
        givenCandidates(List.of(
                tx(1L, KROGER_STORE + FUEL_SUFFIX, AMOUNT, 1, null),
                tx(2L, "KROGER PRIME", AMOUNT, 2, null),
                tx(3L, KROGER_STORE + FUEL_SUFFIX, "-5.00", 3, null),
                tx(4L, KROGER_STORE + FUEL_SUFFIX, "60.00", 4, null),
                tx(5L, "kroger fuel", "50.00", 5, null)));
        givenLinks(Map.of());
        MerchantRuleMatchPreviewRequest andRequest = new MerchantRuleMatchPreviewRequest(
                null, List.of(KROGER, FUEL), MatchType.AND, 0, BigDecimal.TEN, new BigDecimal("50"), null);

        // act
        MerchantRuleMatchPreviewDto result = service.preview(USER_ID, andRequest);

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(2);
        assertThat(result.rows()).extracting(MerchantRuleMatchRowDto::transactionId).containsExactly(5L, 1L);
    }

    @Test
    @DisplayName("a null match type defaults to OR and a null priority defaults to 0")
    void preview_nullMatchTypeAndPriority_defaultToOrAndZero() {
        // arrange -- a priority-1 rule beats the draft, a priority-0 shorter rule loses to it
        givenRules(rule(OTHER_RULE_ID, 1, SHELL), rule(9L, 0, "KROG"));
        givenCandidates(List.of(bare(1L, KROGER_STORE, 1), bare(2L, "SHELL OIL", 2)));
        givenLinks(Map.of());
        MerchantRuleMatchPreviewRequest defaults = new MerchantRuleMatchPreviewRequest(
                null, List.of(KROGER, SHELL), null, null, null, null, null);

        // act
        MerchantRuleMatchPreviewDto result = service.preview(USER_ID, defaults);

        // assert & verify
        assertThat(result.rows()).extracting(MerchantRuleMatchRowDto::transactionId, MerchantRuleMatchRowDto::verdict)
                .containsExactly(
                        tuple(1L, MerchantRuleVerdict.WILL_ASSIGN),
                        tuple(2L, MerchantRuleVerdict.LOSES_TO_OTHER_RULE));
    }

    @Test
    @DisplayName("only the calling user's rules, links and transactions are read, with the draft's non-blank keywords")
    void preview_onlyCallersData() {
        // arrange
        givenRules();
        givenCandidates(List.of());
        givenLinks(Map.of());

        // act
        service.preview(USER_ID, request(null, 0, KROGER, " ", FUEL));

        // assert & verify
        verify(merchantRuleRepository).findByUserId(USER_ID);
        verify(transactionRepository).findCandidatesByDescriptionKeywords(USER_ID, List.of(KROGER, FUEL));
    }

    @Test
    @DisplayName("a ruleId that is not one of the caller's own rules is a not-found, and no transactions are read")
    void preview_foreignOrUnknownRuleId_throwsNotFound() {
        // arrange
        givenRules(rule(OTHER_RULE_ID, 0, KROGER));

        // act & assert & verify
        assertThatThrownBy(() -> service.preview(USER_ID, request(99L, 0, KROGER)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Rule not found");
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("a foreign ruleId is a not-found even when the keywords are empty")
    void preview_foreignRuleIdWithEmptyKeywords_throwsNotFound() {
        // arrange
        givenRules();
        MerchantRuleMatchPreviewRequest empty = new MerchantRuleMatchPreviewRequest(99L, List.of(), null, null, null, null, null);

        // act & assert & verify
        assertThatThrownBy(() -> service.preview(USER_ID, empty))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("the target merchant is validated as the caller's and named in the output")
    void preview_targetMerchant_namedInOutput() {
        // arrange
        givenRules();
        givenCandidates(List.of(bare(1L, KROGER_STORE, 1)));
        givenLinks(Map.of());
        when(merchantService.requireOwnedMerchant(USER_ID, TARGET_ID)).thenReturn(Merchant.builder().id(TARGET_ID).name(KROGER_NAME).build());
        MerchantRuleMatchPreviewRequest withTarget = new MerchantRuleMatchPreviewRequest(
                null, List.of(KROGER), null, null, null, null, TARGET_ID);

        // act
        MerchantRuleMatchPreviewDto result = service.preview(USER_ID, withTarget);

        // assert & verify
        assertThat(result.targetMerchantName()).isEqualTo(KROGER_NAME);
        assertThat(result.willAssignCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("a target merchant that is not the caller's is a not-found, and no transactions are read")
    void preview_foreignTargetMerchant_throwsNotFound() {
        // arrange
        givenRules();
        when(merchantService.requireOwnedMerchant(USER_ID, 99L)).thenThrow(new ResourceNotFoundException("Merchant not found."));
        MerchantRuleMatchPreviewRequest foreign = new MerchantRuleMatchPreviewRequest(
                null, List.of(KROGER), null, null, null, null, 99L);

        // act & assert & verify
        assertThatThrownBy(() -> service.preview(USER_ID, foreign))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Merchant not found.");
        verifyNoInteractions(transactionRepository);
        verify(merchantService, never()).findMatchingMerchantIds(eq(USER_ID), anyList());
    }
}
