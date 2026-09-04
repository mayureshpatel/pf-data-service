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
