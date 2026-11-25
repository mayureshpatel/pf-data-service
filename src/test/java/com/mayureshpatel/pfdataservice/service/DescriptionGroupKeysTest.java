package com.mayureshpatel.pfdataservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link DescriptionGroupKeys}: the key that groups raw descriptions sharing the same
 * merchant text once store numbers, reference codes and a trailing location are stripped, so a
 * backlog is worked once per group, while look-alike merchants (kroger vs kroger fuel) stay apart.
 */
@DisplayName("DescriptionGroupKeys Unit Tests")
class DescriptionGroupKeysTest {

    /** The owner's real-shape kroger fixture: plain stores share one key, fuel centers never join it. */
    @Nested
    @DisplayName("kroger fixture")
    class KrogerFixture {

        @ParameterizedTest
        @CsvSource({
                "'KROGER #431 ROSWELL GA', kroger",
                "'KROGER #512', kroger",
                "'kroger  #122332', kroger",
                "'KROGER FUEL CTR #1431 ROSWELL GA', kroger fuel ctr",
                "'KROGER FUEL CTR1687 SUGAR HILL GA', kroger fuel ctr",
                "'kroger fuel ctr #1223', kroger fuel ctr",
                "'KROGER FUEL #1696', kroger fuel"
        })
        @DisplayName("should cut at the store number so fuel centers keep their extra words")
        void shouldKeyKrogerVariants(String raw, String expected) {
            // arrange -- raw/expected come from the CsvSource

            // act
            String key = DescriptionGroupKeys.key(raw);

            // assert & verify
            assertThat(key).isEqualTo(expected);
        }

        @Test
        @DisplayName("should never give a fuel description the plain kroger key")
        void shouldNotMergeFuelIntoKroger() {
            // arrange
            String plain = DescriptionGroupKeys.key("KROGER #431 ROSWELL GA");

            // act
            String fuel = DescriptionGroupKeys.key("KROGER FUEL #1696");
            String fuelCenter = DescriptionGroupKeys.key("KROGER FUEL CTR1687 SUGAR HILL GA");

            // assert & verify
            assertThat(fuel).isNotEqualTo(plain);
            assertThat(fuelCenter).isNotEqualTo(plain);
        }
    }

    /** Leading reference numbers, mixed tokens and descriptions with no digits at all. */
    @Nested
    @DisplayName("edge shapes")
    class EdgeShapes {

        @ParameterizedTest
        @CsvSource({
                "'1234567 KROGER #431', kroger",
                "'#98765 - KROGER #431', kroger",
                "'NETFLIX.COM', netflix.com",
                "'SPOTIFY USA', spotify usa",
                "'PUBLIX ROSWELL GA', publix roswell",
                "'AMZN MKTP US*2K3AB1', amzn mktp us",
                "'SQ *BLUE BOTTLE 4451', sq *blue bottle",
                "'7-ELEVEN 12345', 7-eleven",
                "'TST* CHIPOTLE 0912 ATLANTA GA', tst* chipotle",
                "'SHELL OIL 57444', shell oil"
        })
        @DisplayName("should keep the merchant words and drop numbers, codes and a trailing state")
        void shouldKeyEdgeShapes(String raw, String expected) {
            // arrange -- raw/expected come from the CsvSource

            // act
            String key = DescriptionGroupKeys.key(raw);

            // assert & verify
            assertThat(key).isEqualTo(expected);
        }

        @Test
        @DisplayName("should fall back to the whole folded description when it is all digits")
        void shouldFallBackWhenAllDigits() {
            // arrange
            String raw = "  0001234 5678 ";

            // act
            String key = DescriptionGroupKeys.key(raw);

            // assert & verify
            assertThat(key).isEqualTo("0001234 5678");
        }

        @Test
        @DisplayName("should keep a lone state-like word rather than empty the key")
        void shouldKeepLoneStateWord() {
            // arrange
            String raw = "GA";

            // act
            String key = DescriptionGroupKeys.key(raw);

            // assert & verify
            assertThat(key).isEqualTo("ga");
        }

        @Test
        @DisplayName("should return an empty key for a null or blank description")
        void shouldReturnEmptyForBlank() {
            // arrange
            String blank = "   ";

            // act
            String fromNull = DescriptionGroupKeys.key(null);
            String fromBlank = DescriptionGroupKeys.key(blank);

            // assert & verify
            assertThat(fromNull).isEmpty();
            assertThat(fromBlank).isEmpty();
        }
    }

    /** The display name keeps the raw spelling's case for the same words the key keeps. */
    @Nested
    @DisplayName("displayName")
    class DisplayName_ {

        @Test
        @DisplayName("should keep the original case of the kept words")
        void shouldKeepCase() {
            // arrange
            String raw = "Kroger Fuel Ctr #1431 Roswell GA";

            // act
            String name = DescriptionGroupKeys.displayName(raw);

            // assert & verify
            assertThat(name).isEqualTo("Kroger Fuel Ctr");
        }
    }
}
