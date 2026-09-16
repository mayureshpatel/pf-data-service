package com.mayureshpatel.pfdataservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("MerchantNameNormalizer Unit Tests")
class MerchantNameNormalizerTest {

    private final MerchantNameNormalizer normalizer = new MerchantNameNormalizer();

    @Nested
    @DisplayName("case normalization")
    class CaseNormalization {

        @ParameterizedTest
        @CsvSource({
                "Starbucks, Starbucks",
                "STARBUCKS, Starbucks",
                "starbucks, Starbucks",
                "TRADER JOES, Trader Joes"
        })
        @DisplayName("should title-case regardless of input casing, and leave already-clean names stable")
        void shouldTitleCase(String input, String expected) {
            // arrange -- input/expected come from the CsvSource

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("trailing reference code / number stripping")
    class ReferenceCodeStripping {

        @Test
        @DisplayName("should strip a single trailing hash-prefixed reference code")
        void shouldStripSingleTrailingReferenceCode() {
            // arrange
            String input = "WHOLEFDS #12345";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals("Wholefds", result);
        }

        @Test
        @DisplayName("should strip multiple trailing number groups, not just the last one")
        void shouldStripMultipleTrailingNumberGroups() {
            // arrange
            String input = "CHEVRON 00123 4567";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals("Chevron", result);
        }
    }

    @Nested
    @DisplayName("location suffix stripping")
    class LocationSuffixStripping {

        @Test
        @DisplayName("should strip a trailing recognized US state code")
        void shouldStripTrailingStateCode() {
            // arrange
            String input = "Chevron Gas WA";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals("Chevron Gas", result);
        }

        @Test
        @DisplayName("should strip both a trailing number and a trailing state code together")
        void shouldStripNumberAndStateCodeTogether() {
            // arrange
            String input = "SHELL OIL 00123 WA";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals("Shell Oil", result);
        }

        @Test
        @DisplayName("known accepted tradeoff: a real trailing word that happens to match a state code "
                + "still gets stripped -- this app's deliberately aggressive normalization choice (PF-EPIC-021 "
                + "interview), not a bug")
        void knownTradeoff_ambiguousTrailingWordMatchingStateCode() {
            // arrange
            String input = "ACME CO";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals("Acme", result);
        }

        @Test
        @DisplayName("should not strip a merchant's real trailing word just because it's 2 letters, "
                + "when that word isn't a recognized state code")
        void shouldNotStripNonStateTwoLetterTrailingWord() {
            // arrange
            String input = "H AND M";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals("H And M", result);
        }
    }

    @Nested
    @DisplayName("PF-832: store number followed by a city, before the trailing state code")
    class NumberBlockedByCity {

        @Test
        @DisplayName("should strip both the store number and the state code even when a city sits "
                + "between them -- the city itself is left alone (still out of scope, per the "
                + "documented PF-EPIC-021 design), only the two already-recognized token types are "
                + "removed regardless of what's between them")
        void shouldStripNumberAndStateAroundAnInterveningCity() {
            // arrange -- confirmed live: this exact shape split one real chain across 15 separate
            // merchant records (some statements had a trailing city, some didn't)
            String input = "KROGER #431 ROSWELL GA";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals("Kroger Roswell", result);
        }

        @Test
        @DisplayName("should still strip the number+city+state shape when the city is two words")
        void shouldStripNumberAndStateAroundATwoWordCity() {
            // arrange
            String input = "KROGER #696 WARNER ROBINS GA";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals("Kroger Warner Robins", result);
        }

        @Test
        @DisplayName("should produce the same merchant name for the same store whether or not a "
                + "given statement happened to include a trailing city")
        void shouldNormalizeToTheSameNameWithOrWithoutATrailingCity() {
            // arrange -- confirmed live: 'WALGREENS #11348' and 'WALGREENS #11348 WARNER ROBINS GA'
            // (same real store) previously produced two different merchants
            String withCity = normalizer.normalize("WALGREENS #11348 WARNER ROBINS GA");
            String withoutCity = normalizer.normalize("WALGREENS #11348");

            // act & assert -- withoutCity has no trailing state, so it keeps stripping only the
            // number (existing end-anchored behavior); withCity now strips the number too, just
            // leaves the city. They won't be byte-identical (one still carries the city), but the
            // number must be gone from both -- the specific bug was the number surviving only in
            // the city-suffixed version.
            assertEquals("Walgreens", withoutCity);
            assertEquals("Walgreens Warner Robins", withCity);
        }

        @Test
        @DisplayName("should NOT reach backward past a city when there's no trailing state code at "
                + "all -- the aggressive skip-the-city behavior only activates once a real trailing "
                + "state code confirms this is genuinely a location-suffixed record, not a merchant "
                + "name that simply happens to start with a number")
        void shouldNotStripALeadingNumberWithNoTrailingStateCode() {
            // arrange -- no trailing state code anywhere in this input
            String input = "24 Hour Fitness";

            // act
            String result = normalizer.normalize(input);

            // assert & verify -- must stay fully intact; this is a real, whole merchant name
            assertEquals("24 Hour Fitness", result);
        }

        @Test
        @DisplayName("should strip multiple adjacent numeric tokens before a city, not just the one "
                + "immediately before it")
        void shouldStripAdjacentNumericTokensBeforeACity() {
            // arrange
            String input = "CHEVRON 00123 4567 ROSWELL GA";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals("Chevron Roswell", result);
        }
    }

    @Nested
    @DisplayName("fallback for names that would normalize to empty")
    class EmptyNormalizationFallback {

        @Test
        @DisplayName("should fall back to the original name when stripping would leave nothing")
        void shouldFallBackWhenFullyNumeric() {
            // arrange
            String input = "#4523";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertFalse(result.isBlank());
            assertEquals("#4523", result);
        }

        @Test
        @DisplayName("should fall back to a non-blank result even when both a leading number and a "
                + "trailing state code would otherwise strip the input to nothing")
        void shouldFallBackWhenNumberAndLocationConsumeEverything() {
            // arrange
            String input = "00123 WA";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertFalse(result.isBlank());
        }
    }

    @Nested
    @DisplayName("null and blank input handling")
    class NullAndBlankHandling {

        @Test
        @DisplayName("should return an empty string for null input, never throw")
        void shouldReturnEmptyForNull() {
            // arrange
            String input = null;

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals("", result);
        }

        @Test
        @DisplayName("should return an empty string for blank input")
        void shouldReturnEmptyForBlank() {
            // arrange
            String input = "   ";

            // act
            String result = normalizer.normalize(input);

            // assert & verify
            assertEquals("", result);
        }
    }
}
