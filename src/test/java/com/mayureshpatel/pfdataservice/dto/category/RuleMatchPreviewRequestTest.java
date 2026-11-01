package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.dto.ValidatorTestBase;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link RuleMatchPreviewRequest}'s bean-validation constraints (PF-900), which mirror
 * {@link CategoryRuleCreateRequest} and add a 20-keyword cap, since the candidate query silently
 * ignores keywords past 20 and the request must reject them instead.
 */
@DisplayName("RuleMatchPreviewRequest Validation Tests")
class RuleMatchPreviewRequestTest extends ValidatorTestBase {

    private static RuleMatchPreviewRequest withKeywords(List<String> keywords) {
        return new RuleMatchPreviewRequest(null, keywords, MatchType.OR, 0, null, null);
    }

    private static Set<String> messages(RuleMatchPreviewRequest request) {
        Set<ConstraintViolation<RuleMatchPreviewRequest>> violations = validator().validate(request);
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("a full valid request, 20 keywords, and an empty keyword list all pass")
    void validRequests_pass() {
        // arrange
        List<String> twenty = IntStream.range(0, 20).mapToObj(i -> "KW" + i).toList();
        RuleMatchPreviewRequest full = new RuleMatchPreviewRequest(
                7L, List.of("AMAZON"), MatchType.AND, 3, BigDecimal.ZERO, new BigDecimal("50"));

        // act & assert & verify
        assertThat(messages(full)).isEmpty();
        assertThat(messages(withKeywords(twenty))).isEmpty();
        assertThat(messages(withKeywords(List.of()))).isEmpty();
        assertThat(messages(new RuleMatchPreviewRequest(null, List.of("A"), null, null, null, null))).isEmpty();
    }

    @Test
    @DisplayName("more than 20 keywords are rejected")
    void tooManyKeywords_rejected() {
        // arrange
        List<String> twentyOne = IntStream.range(0, 21).mapToObj(i -> "KW" + i).toList();

        // act
        Set<String> result = messages(withKeywords(twentyOne));

        // assert & verify
        assertThat(result).containsExactly("A rule can have at most 20 keywords.");
    }

    @Test
    @DisplayName("null keywords are rejected")
    void nullKeywords_rejected() {
        // act
        Set<String> result = messages(withKeywords(null));

        // assert & verify
        assertThat(result).containsExactly("Keywords cannot be null.");
    }

    @Test
    @DisplayName("a blank keyword is rejected")
    void blankKeyword_rejected() {
        // act
        Set<String> result = messages(withKeywords(List.of("AMAZON", "  ")));

        // assert & verify
        assertThat(result).containsExactly("Keyword cannot be blank.");
    }

    @Test
    @DisplayName("a keyword longer than 255 characters is rejected")
    void longKeyword_rejected() {
        // act
        Set<String> result = messages(withKeywords(List.of(String.join("", Collections.nCopies(256, "a")))));

        // assert & verify
        assertThat(result).containsExactly("Keyword cannot exceed 255 characters.");
    }

    @Test
    @DisplayName("a negative priority is rejected")
    void negativePriority_rejected() {
        // act
        Set<String> result = messages(new RuleMatchPreviewRequest(null, List.of("A"), null, -1, null, null));

        // assert & verify
        assertThat(result).containsExactly("Priority must be a positive number or zero.");
    }

    @Test
    @DisplayName("negative amount bounds are rejected")
    void negativeAmounts_rejected() {
        // act
        Set<String> result = messages(new RuleMatchPreviewRequest(
                null, List.of("A"), null, 0, new BigDecimal("-1"), new BigDecimal("-2")));

        // assert & verify
        assertThat(result).containsExactlyInAnyOrder(
                "Minimum amount must be a positive number or zero.",
                "Maximum amount must be a positive number or zero.");
    }
}
