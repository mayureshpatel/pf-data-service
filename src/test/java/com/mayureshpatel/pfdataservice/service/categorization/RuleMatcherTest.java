package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies {@code RuleMatcher}'s keyword (AND/OR, case-insensitive) and absolute-amount-range matching of a single rule. */
@DisplayName("RuleMatcher Unit Tests")
class RuleMatcherTest {

    private static final String DESCRIPTION = "AMAZON MARKETPLACE PURCHASE";
    private static final String KEYWORD_AMAZON = "amazon";
    private static final String MAX_AMOUNT = "20.00";
    private static final String MIN_AMOUNT = "5.00";

    private final RuleMatcher matcher = new RuleMatcher();

    /** An OR rule matches when any one keyword is present, ignoring case. */
    @Test
    @DisplayName("matches_orRule_anyKeywordCaseInsensitive")
    void matches_orRule_anyKeywordCaseInsensitive() {
        // arrange
        CategoryRule rule = CategoryRule.builder().keywords(List.of("netflix", KEYWORD_AMAZON)).matchType(MatchType.OR).build();

        // act
        boolean result = matcher.matches(DESCRIPTION, null, rule);

        // assert & verify
        assertTrue(result);
    }

    /** An AND rule matches only when every keyword is present. */
    @Test
    @DisplayName("matches_andRule_requiresAllKeywords")
    void matches_andRule_requiresAllKeywords() {
        // arrange
        CategoryRule both = CategoryRule.builder().keywords(List.of(KEYWORD_AMAZON, "purchase")).matchType(MatchType.AND).build();
        CategoryRule oneMissing = CategoryRule.builder().keywords(List.of(KEYWORD_AMAZON, "netflix")).matchType(MatchType.AND).build();

        // act
        boolean bothResult = matcher.matches(DESCRIPTION, null, both);
        boolean missingResult = matcher.matches(DESCRIPTION, null, oneMissing);

        // assert & verify
        assertTrue(bothResult);
        assertFalse(missingResult);
    }

    /** An empty or null keyword list never matches, even under AND. */
    @Test
    @DisplayName("matches_noKeywords_neverMatches")
    void matches_noKeywords_neverMatches() {
        // arrange
        CategoryRule empty = CategoryRule.builder().keywords(List.of()).matchType(MatchType.AND).build();
        CategoryRule nullKeywords = CategoryRule.builder().keywords(null).matchType(MatchType.OR).build();

        // act
        boolean emptyResult = matcher.matches(DESCRIPTION, null, empty);
        boolean nullResult = matcher.matches(DESCRIPTION, null, nullKeywords);

        // assert & verify
        assertFalse(emptyResult);
        assertFalse(nullResult);
    }

    /** The amount range compares the absolute amount and is inclusive at both bounds. */
    @Test
    @DisplayName("matches_amountRange_usesAbsoluteMagnitudeInclusive")
    void matches_amountRange_usesAbsoluteMagnitudeInclusive() {
        // arrange
        CategoryRule rule = CategoryRule.builder().keywords(List.of(KEYWORD_AMAZON)).matchType(MatchType.OR)
                .minAmount(new BigDecimal(MIN_AMOUNT)).maxAmount(new BigDecimal(MAX_AMOUNT)).build();

        // act & assert & verify
        assertTrue(matcher.matches(DESCRIPTION, new BigDecimal("-20.00"), rule));
        assertFalse(matcher.matches(DESCRIPTION, new BigDecimal("-20.01"), rule));
        assertTrue(matcher.matches(DESCRIPTION, new BigDecimal("-5.00"), rule));
        assertFalse(matcher.matches(DESCRIPTION, new BigDecimal("-4.99"), rule));
    }

    /** A rule with a range never matches a null amount. */
    @Test
    @DisplayName("matches_rangeSetButNullAmount_false")
    void matches_rangeSetButNullAmount_false() {
        // arrange
        CategoryRule rule = CategoryRule.builder().keywords(List.of(KEYWORD_AMAZON)).matchType(MatchType.OR)
                .maxAmount(new BigDecimal(MAX_AMOUNT)).build();

        // act
        boolean result = matcher.matches(DESCRIPTION, null, rule);

        // assert & verify
        assertFalse(result);
    }

    /** A rule without a range matches on keyword alone, whatever the amount. */
    @Test
    @DisplayName("matches_noRange_keywordAloneSuffices")
    void matches_noRange_keywordAloneSuffices() {
        // arrange
        CategoryRule rule = CategoryRule.builder().keywords(List.of(KEYWORD_AMAZON)).matchType(MatchType.OR).build();

        // act
        boolean result = matcher.matches(DESCRIPTION, new BigDecimal("-9999.00"), rule);

        // assert & verify
        assertTrue(result);
    }

    /** A null description never matches. */
    @Test
    @DisplayName("matches_nullDescription_false")
    void matches_nullDescription_false() {
        // arrange
        CategoryRule rule = CategoryRule.builder().keywords(List.of(KEYWORD_AMAZON)).matchType(MatchType.OR).build();

        // act
        boolean result = matcher.matches(null, new BigDecimal("1.00"), rule);

        // assert & verify
        assertFalse(result);
    }
}
