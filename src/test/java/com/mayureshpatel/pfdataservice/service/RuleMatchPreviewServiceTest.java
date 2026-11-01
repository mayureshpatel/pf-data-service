package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.category.RuleMatchPreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.RuleMatchPreviewRequest;
import com.mayureshpatel.pfdataservice.dto.category.RuleMatchRowDto;
import com.mayureshpatel.pfdataservice.dto.category.RuleRefDto;
import com.mayureshpatel.pfdataservice.dto.category.RuleVerdict;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
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
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link RuleMatchPreviewService}'s PF-900 verdicts, counts, row cap and ordering, using
 * mocked repositories and the real {@link RuleMatcher} so matching semantics are the import ones.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleMatchPreviewService Unit Tests")
class RuleMatchPreviewServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long EDITED_RULE_ID = 7L;
    private static final Long OTHER_RULE_ID = 8L;
    private static final String AMAZON = "AMAZON";
    private static final String SHOPPING = "Shopping";
    private static final String AMAZON_MKTP = "AMAZON MKTP US*123";
    private static final String MKTP = "MKTP";
    private static final String GROCERIES = "Groceries";
    private static final String AMOUNT = "-25.00";
    private static final OffsetDateTime BASE_DATE = OffsetDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private CategoryRuleRepository categoryRuleRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private RuleMatchPreviewService service;

    @BeforeEach
    void setUp() {
        service = new RuleMatchPreviewService(categoryRuleRepository, transactionRepository, new RuleMatcher());
    }

    private static Transaction tx(long id, String description, String amount, int day, Category category) {
        return Transaction.builder()
                .id(id)
                .description(description)
                .amount(new BigDecimal(amount))
                .transactionDate(BASE_DATE.plusDays(day))
                .account(Account.builder().id(3L).name("Checking").build())
                .category(category)
                .build();
    }

    private static Transaction uncategorized(long id, String description, int day) {
        return tx(id, description, AMOUNT, day, null);
    }

    private static CategoryRule rule(Long id, int priority, String... keywords) {
        return CategoryRule.builder()
                .id(id)
                .keywords(List.of(keywords))
                .matchType(MatchType.OR)
                .priority(priority)
                .category(Category.builder().id(50L).name(SHOPPING).build())
                .build();
    }

    private static RuleMatchPreviewRequest request(Long ruleId, Integer priority, String... keywords) {
        return new RuleMatchPreviewRequest(ruleId, List.of(keywords), MatchType.OR, priority, null, null);
    }

    private void givenRules(CategoryRule... rules) {
        when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rules));
    }

    private void givenCandidates(List<Transaction> candidates) {
        when(transactionRepository.findCandidatesByDescriptionKeywords(eq(USER_ID), anyList())).thenReturn(candidates);
    }

    @Test
    @DisplayName("blank or empty keywords return an empty preview without touching any repository")
    void preview_blankKeywords_returnsEmptyAndNeverQueries() {
        // arrange
        RuleMatchPreviewRequest blank = request(null, 0, "  ", "");
        RuleMatchPreviewRequest empty = new RuleMatchPreviewRequest(null, List.of(), null, null, null, null);
        RuleMatchPreviewRequest nullKeywords = new RuleMatchPreviewRequest(null, null, null, null, null, null);

        // act
        List<RuleMatchPreviewDto> results = List.of(
                service.preview(USER_ID, blank),
                service.preview(USER_ID, empty),
                service.preview(USER_ID, nullKeywords));

        // assert & verify
        assertThat(results).allSatisfy(result -> {
            assertThat(result.totalMatches()).isZero();
            assertThat(result.willCategorizeCount()).isZero();
            assertThat(result.losesToOtherRuleCount()).isZero();
            assertThat(result.alreadyCategorizedCount()).isZero();
            assertThat(result.truncated()).isFalse();
            assertThat(result.rows()).isEmpty();
        });
        verifyNoInteractions(categoryRuleRepository, transactionRepository);
    }

    @Test
    @DisplayName("an uncategorized transaction only the draft matches is will-categorize; rows the draft does not match are dropped")
    void preview_uncategorizedMatchedOnlyByDraft_willCategorize() {
        // arrange
        givenRules(rule(OTHER_RULE_ID, 5, "SHELL"));
        givenCandidates(List.of(uncategorized(1L, AMAZON_MKTP, 1), uncategorized(2L, "SHELL OIL 123", 2)));

        // act
        RuleMatchPreviewDto result = service.preview(USER_ID, request(null, 0, "amazon"));

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(1);
        assertThat(result.willCategorizeCount()).isEqualTo(1);
        assertThat(result.losesToOtherRuleCount()).isZero();
        assertThat(result.alreadyCategorizedCount()).isZero();
        assertThat(result.truncated()).isFalse();
        assertThat(result.rows()).hasSize(1);
        RuleMatchRowDto row = result.rows().get(0);
        assertThat(row.transactionId()).isEqualTo(1L);
        assertThat(row.description()).isEqualTo(AMAZON_MKTP);
        assertThat(row.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(row.date()).isEqualTo(BASE_DATE.plusDays(1));
        assertThat(row.accountName()).isEqualTo("Checking");
        assertThat(row.currentCategory()).isNull();
        assertThat(row.verdict()).isEqualTo(RuleVerdict.WILL_CATEGORIZE);
        assertThat(row.otherRules()).isEmpty();
    }

    @Test
    @DisplayName("a higher-priority existing rule that matches first wins, and the row names every other matching rule in order")
    void preview_higherPriorityExistingRule_losesAndNamesThatRule() {
        // arrange
        CategoryRule high = rule(OTHER_RULE_ID, 10, AMAZON);
        CategoryRule low = rule(9L, 1, MKTP);
        givenRules(high, low);
        givenCandidates(List.of(uncategorized(1L, AMAZON_MKTP, 1)));

        // act
        RuleMatchPreviewDto result = service.preview(USER_ID, request(null, 5, "amazon mktp"));

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(1);
        assertThat(result.willCategorizeCount()).isZero();
        assertThat(result.losesToOtherRuleCount()).isEqualTo(1);
        RuleMatchRowDto row = result.rows().get(0);
        assertThat(row.verdict()).isEqualTo(RuleVerdict.LOSES_TO_OTHER_RULE);
        assertThat(row.otherRules()).containsExactly(
                new RuleRefDto(OTHER_RULE_ID, List.of(AMAZON), SHOPPING, 10),
                new RuleRefDto(9L, List.of(MKTP), SHOPPING, 1));
    }

    @Test
    @DisplayName("at equal priority and specificity a new draft ranks after the existing rule (draft id sorts last)")
    void preview_equalPriorityAndSpecificity_newDraftLoses_toExistingRule() {
        // arrange
        givenRules(rule(OTHER_RULE_ID, 0, AMAZON));
        givenCandidates(List.of(uncategorized(1L, AMAZON_MKTP, 1)));

        // act
        RuleMatchPreviewDto result = service.preview(USER_ID, request(null, 0, "amazon"));

        // assert & verify
        assertThat(result.losesToOtherRuleCount()).isEqualTo(1);
        assertThat(result.willCategorizeCount()).isZero();
        assertThat(result.rows().get(0).verdict()).isEqualTo(RuleVerdict.LOSES_TO_OTHER_RULE);
        assertThat(result.rows().get(0).otherRules()).extracting(RuleRefDto::id).containsExactly(OTHER_RULE_ID);
    }

    @Test
    @DisplayName("a categorized transaction the draft matches is already-categorized, never counted as will-change")
    void preview_categorizedTransaction_alreadyCategorizedNeverWillChange() {
        // arrange
        Category groceries = Category.builder().id(60L).userId(USER_ID).name(GROCERIES).type("EXPENSE").build();
        givenRules(rule(OTHER_RULE_ID, 0, MKTP));
        givenCandidates(List.of(tx(1L, AMAZON_MKTP, AMOUNT, 1, groceries)));

        // act
        RuleMatchPreviewDto result = service.preview(USER_ID, request(null, 99, "amazon"));

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(1);
        assertThat(result.alreadyCategorizedCount()).isEqualTo(1);
        assertThat(result.willCategorizeCount()).isZero();
        assertThat(result.losesToOtherRuleCount()).isZero();
        RuleMatchRowDto row = result.rows().get(0);
        assertThat(row.verdict()).isEqualTo(RuleVerdict.ALREADY_CATEGORIZED);
        assertThat(row.currentCategory().id()).isEqualTo(60L);
        assertThat(row.currentCategory().name()).isEqualTo(GROCERIES);
        assertThat(row.otherRules()).extracting(RuleRefDto::id).containsExactly(OTHER_RULE_ID);
    }

    @Test
    @DisplayName("editing a rule replaces its saved version in the ordering, so the rule is never compared with itself")
    void preview_editingRule_replacesSavedVersion_notComparedWithItself() {
        // arrange -- the saved version has the same priority and keyword, so if it stayed in the
        // ordering it would tie with the draft; it also matches a row the edited draft no longer does
        givenRules(rule(EDITED_RULE_ID, 0, AMAZON, "OLDKEYWORD"));
        givenCandidates(List.of(uncategorized(1L, AMAZON_MKTP, 1), uncategorized(2L, "OLDKEYWORD STORE", 2)));

        // act
        RuleMatchPreviewDto result = service.preview(USER_ID, request(EDITED_RULE_ID, 0, AMAZON));

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(1);
        assertThat(result.willCategorizeCount()).isEqualTo(1);
        assertThat(result.losesToOtherRuleCount()).isZero();
        assertThat(result.rows()).singleElement().satisfies(row -> {
            assertThat(row.transactionId()).isEqualTo(1L);
            assertThat(row.verdict()).isEqualTo(RuleVerdict.WILL_CATEGORIZE);
            assertThat(row.otherRules()).isEmpty();
        });
    }

    @Test
    @DisplayName("changing an edited rule's priority flips its verdict against another rule")
    void preview_editingRule_priorityChangeFlipsVerdict() {
        // arrange
        when(categoryRuleRepository.findByUserId(USER_ID))
                .thenReturn(List.of(rule(OTHER_RULE_ID, 5, AMAZON), rule(EDITED_RULE_ID, 1, AMAZON)));
        givenCandidates(List.of(uncategorized(1L, AMAZON_MKTP, 1)));

        // act
        RuleMatchPreviewDto unchanged = service.preview(USER_ID, request(EDITED_RULE_ID, 1, AMAZON));
        RuleMatchPreviewDto raised = service.preview(USER_ID, request(EDITED_RULE_ID, 9, AMAZON));

        // assert & verify
        assertThat(unchanged.losesToOtherRuleCount()).isEqualTo(1);
        assertThat(unchanged.rows().get(0).verdict()).isEqualTo(RuleVerdict.LOSES_TO_OTHER_RULE);
        assertThat(unchanged.rows().get(0).otherRules()).extracting(RuleRefDto::id).containsExactly(OTHER_RULE_ID);
        assertThat(raised.willCategorizeCount()).isEqualTo(1);
        assertThat(raised.rows().get(0).verdict()).isEqualTo(RuleVerdict.WILL_CATEGORIZE);
        assertThat(raised.rows().get(0).otherRules()).extracting(RuleRefDto::id).containsExactly(OTHER_RULE_ID);
    }

    @Test
    @DisplayName("120 matches give 50 rows and full counts, will-categorize first then losing, newest first within each")
    void preview_over50Matches_truncatedRowsAndFullCounts() {
        // arrange -- 20 will-categorize, 60 losing (also matched by a higher-priority rule), 40
        // already categorized, shuffled so input order cannot explain the output order
        Category groceries = Category.builder().id(60L).name(GROCERIES).build();
        List<Transaction> candidates = new ArrayList<>();
        long id = 1;
        for (int i = 0; i < 20; i++) {
            candidates.add(uncategorized(id++, "AMAZON " + i, i * 6));
        }
        for (int i = 0; i < 60; i++) {
            candidates.add(uncategorized(id++, "AMAZON PRIME " + i, i * 2 + 1));
        }
        for (int i = 0; i < 40; i++) {
            candidates.add(tx(id++, "AMAZON " + i, "-10.00", i * 3 + 2, groceries));
        }
        Collections.shuffle(candidates, new Random(42));
        givenRules(rule(OTHER_RULE_ID, 10, "PRIME"));
        givenCandidates(candidates);

        // act
        RuleMatchPreviewDto result = service.preview(USER_ID, request(null, 0, AMAZON));

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(120);
        assertThat(result.willCategorizeCount()).isEqualTo(20);
        assertThat(result.losesToOtherRuleCount()).isEqualTo(60);
        assertThat(result.alreadyCategorizedCount()).isEqualTo(40);
        assertThat(result.truncated()).isTrue();
        assertThat(result.rows()).hasSize(RuleMatchPreviewService.MAX_ROWS);
        List<RuleMatchRowDto> will = result.rows().subList(0, 20);
        List<RuleMatchRowDto> loses = result.rows().subList(20, 50);
        assertThat(will).allMatch(row -> row.verdict() == RuleVerdict.WILL_CATEGORIZE);
        assertThat(loses).allMatch(row -> row.verdict() == RuleVerdict.LOSES_TO_OTHER_RULE);
        assertThat(will).isSortedAccordingTo(Comparator.comparing(RuleMatchRowDto::date).reversed());
        assertThat(loses).isSortedAccordingTo(Comparator.comparing(RuleMatchRowDto::date).reversed());
        // the losing rows kept are the 30 newest of the 60 (days 119 down to 61)
        assertThat(loses.get(0).date()).isEqualTo(BASE_DATE.plusDays(119));
        assertThat(loses.get(29).date()).isEqualTo(BASE_DATE.plusDays(61));
    }

    @Test
    @DisplayName("rows are ordered will-categorize, then losing, then already categorized, newest first within each")
    void preview_rowsOrderedByVerdictThenNewestFirst() {
        // arrange
        Category groceries = Category.builder().id(60L).name(GROCERIES).build();
        givenRules(rule(OTHER_RULE_ID, 10, "PRIME"));
        givenCandidates(List.of(
                tx(1L, AMAZON, "-1.00", 9, groceries),
                uncategorized(2L, "AMAZON PRIME", 8),
                uncategorized(3L, AMAZON, 1),
                uncategorized(4L, "AMAZON PRIME", 2),
                tx(5L, AMAZON, "-1.00", 3, groceries),
                uncategorized(6L, AMAZON, 5)));

        // act
        RuleMatchPreviewDto result = service.preview(USER_ID, request(null, 0, AMAZON));

        // assert & verify
        assertThat(result.truncated()).isFalse();
        assertThat(result.rows()).extracting(RuleMatchRowDto::transactionId).containsExactly(6L, 3L, 2L, 4L, 1L, 5L);
    }

    @Test
    @DisplayName("an AND draft with an amount range matches through the shared RuleMatcher (absolute amount, inclusive bounds, case-insensitive)")
    void preview_andRuleAndAmountRange_useRuleMatcher() {
        // arrange
        givenRules();
        givenCandidates(List.of(
                tx(1L, AMAZON_MKTP, AMOUNT, 1, null),
                tx(2L, "AMAZON PRIME", AMOUNT, 2, null),
                tx(3L, AMAZON_MKTP, "-5.00", 3, null),
                tx(4L, AMAZON_MKTP, "60.00", 4, null),
                tx(5L, "amazon mktp", "50.00", 5, null)));
        RuleMatchPreviewRequest andRequest = new RuleMatchPreviewRequest(
                null, List.of(AMAZON, MKTP), MatchType.AND, 0, BigDecimal.TEN, new BigDecimal("50"));

        // act
        RuleMatchPreviewDto result = service.preview(USER_ID, andRequest);

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(2);
        assertThat(result.willCategorizeCount()).isEqualTo(2);
        assertThat(result.rows()).extracting(RuleMatchRowDto::transactionId).containsExactly(5L, 1L);
    }

    @Test
    @DisplayName("a null match type defaults to OR and a null priority defaults to 0")
    void preview_nullMatchTypeAndPriority_defaultToOrAndZero() {
        // arrange -- a priority-1 rule beats the draft, a priority-0 shorter rule loses to it
        givenRules(rule(OTHER_RULE_ID, 1, "SHELL"), rule(9L, 0, "AMAZO"));
        givenCandidates(List.of(uncategorized(1L, AMAZON_MKTP, 1), uncategorized(2L, "SHELL OIL", 2)));
        RuleMatchPreviewRequest defaults = new RuleMatchPreviewRequest(
                null, List.of(AMAZON, "SHELL"), null, null, null, null);

        // act
        RuleMatchPreviewDto result = service.preview(USER_ID, defaults);

        // assert & verify
        assertThat(result.totalMatches()).isEqualTo(2);
        assertThat(result.willCategorizeCount()).isEqualTo(1);
        assertThat(result.losesToOtherRuleCount()).isEqualTo(1);
        assertThat(result.rows()).extracting(RuleMatchRowDto::transactionId, RuleMatchRowDto::verdict)
                .containsExactly(
                        tuple(1L, RuleVerdict.WILL_CATEGORIZE),
                        tuple(2L, RuleVerdict.LOSES_TO_OTHER_RULE));
    }

    @Test
    @DisplayName("only the calling user's rules and transactions are read, with the draft's non-blank keywords")
    void preview_onlyCallersTransactionsAndRules() {
        // arrange
        givenRules();
        givenCandidates(List.of());

        // act
        service.preview(USER_ID, request(null, 0, AMAZON, " ", MKTP));

        // assert & verify
        verify(categoryRuleRepository).findByUserId(USER_ID);
        verify(transactionRepository).findCandidatesByDescriptionKeywords(USER_ID, List.of(AMAZON, MKTP));
        verifyNoMoreInteractions(categoryRuleRepository, transactionRepository);
    }

    @Test
    @DisplayName("a ruleId that is not one of the caller's own rules is a not-found, and no transactions are read")
    void preview_foreignOrUnknownRuleId_throwsNotFound() {
        // arrange -- rule 99 belongs to someone else (or does not exist); it is not in the caller's rules
        givenRules(rule(OTHER_RULE_ID, 0, AMAZON));

        // act & assert & verify
        assertThatThrownBy(() -> service.preview(USER_ID, request(99L, 0, AMAZON)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Rule not found");
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("a foreign ruleId is a not-found even when the keywords are empty")
    void preview_foreignRuleIdWithEmptyKeywords_throwsNotFound() {
        // arrange
        givenRules();
        RuleMatchPreviewRequest empty = new RuleMatchPreviewRequest(99L, List.of(), null, null, null, null);

        // act & assert & verify
        assertThatThrownBy(() -> service.preview(USER_ID, empty))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(transactionRepository);
    }
}
