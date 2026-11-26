package com.mayureshpatel.pfdataservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Verifies {@link MerchantNameSimilarity}: exact, prefix and abbreviation word matches, and that
 * extra words on either side lower the score so the closest merchant ranks first.
 */
@DisplayName("MerchantNameSimilarity Unit Tests")
class MerchantNameSimilarityTest {

    @Test
    @DisplayName("should rank Kroger Fuel Center above Kroger for the fuel group, and the reverse for the plain group")
    void shouldRankKrogerLookAlikes() {
        // arrange
        String fuelGroup = "KROGER FUEL CTR";
        String plainGroup = "KROGER";

        // act
        double fuelToFuel = MerchantNameSimilarity.score(fuelGroup, "Kroger Fuel Center");
        double fuelToPlain = MerchantNameSimilarity.score(fuelGroup, "Kroger");
        double plainToPlain = MerchantNameSimilarity.score(plainGroup, "Kroger");
        double plainToFuel = MerchantNameSimilarity.score(plainGroup, "Kroger Fuel Center");

        // assert & verify
        assertThat(fuelToFuel).isCloseTo(2.75 / 3, within(1e-9));
        assertThat(fuelToPlain).isCloseTo(1.0 / 3, within(1e-9));
        assertThat(plainToPlain).isEqualTo(1.0);
        assertThat(plainToFuel).isCloseTo(1.0 / 3, within(1e-9));
    }

    @ParameterizedTest
    @CsvSource({
            "'WAL', 'Walmart', 0.9",
            "'AMZN MKTP US', 'Amazon', 0.25",
            "'NETFLIX.COM', 'Netflix', 0.5",
            "'SQ *BLUE BOTTLE', 'Blue Bottle Coffee', 0.5"
    })
    @DisplayName("should score prefixes and abbreviations below exact words")
    void shouldScorePartialWords(String group, String merchant, double expected) {
        // arrange -- group/merchant/expected come from the CsvSource

        // act
        double score = MerchantNameSimilarity.score(group, merchant);

        // assert & verify
        assertThat(score).isCloseTo(expected, within(1e-9));
    }

    @Test
    @DisplayName("should not count short words as prefixes or a merchant word twice")
    void shouldGuardShortAndReusedWords() {
        // arrange
        String group = "US US";

        // act
        double shortPrefix = MerchantNameSimilarity.score("A", "Apple");
        double reused = MerchantNameSimilarity.score(group, "US Bank");

        // assert & verify
        assertThat(shortPrefix).isZero();
        assertThat(reused).isCloseTo(1.0 / 3, within(1e-9));
    }

    @Test
    @DisplayName("should score zero for no word in common, blank or null names")
    void shouldScoreZero() {
        // arrange
        String group = "SHELL OIL";

        // act
        double unrelated = MerchantNameSimilarity.score(group, "Publix");
        double blank = MerchantNameSimilarity.score("  ", "Publix");
        double nullName = MerchantNameSimilarity.score(group, null);

        // assert & verify
        assertThat(unrelated).isZero();
        assertThat(blank).isZero();
        assertThat(nullName).isZero();
    }
}
