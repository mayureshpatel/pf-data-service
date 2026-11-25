package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.transaction.DescriptionGroupDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDescriptionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link DescriptionGrouper}: folding a flat, usage-ordered description list into groups,
 * filtering by an include phrase and {@code -exclude} terms, marking look-alike groups as similar,
 * and ordering so similar groups sit next to each other.
 */
@DisplayName("DescriptionGrouper Unit Tests")
class DescriptionGrouperTest {

    private static TransactionDescriptionDto row(String description, long count) {
        return new TransactionDescriptionDto(description, count, null, null);
    }

    private static final List<TransactionDescriptionDto> KROGER_FIXTURE = List.of(
            row("KROGER #431 ROSWELL GA", 105),
            row("KROGER #512", 100),
            row("PUBLIX #1201", 60),
            row("KROGER FUEL CTR #1431 ROSWELL GA", 38),
            row("KROGER FUEL CTR1687 SUGAR HILL GA", 10),
            row("KROGER FUEL #1696", 4)
    );

    /** Variants with the same key become one group with summed counts. */
    @Nested
    @DisplayName("grouping")
    class Grouping {

        @Test
        @DisplayName("should group store variants and keep fuel centers apart")
        void shouldGroupKrogerFixture() {
            // arrange
            List<TransactionDescriptionDto> rows = KROGER_FIXTURE;

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(rows, null);

            // assert & verify
            assertThat(groups).extracting(DescriptionGroupDto::key)
                    .containsExactly("kroger", "kroger fuel ctr", "kroger fuel", "publix");
            DescriptionGroupDto kroger = groups.get(0);
            assertThat(kroger.variantCount()).isEqualTo(2);
            assertThat(kroger.transactionCount()).isEqualTo(205);
            assertThat(kroger.displayName()).isEqualTo("KROGER");
            assertThat(kroger.variants()).extracting(TransactionDescriptionDto::description)
                    .containsExactly("KROGER #431 ROSWELL GA", "KROGER #512");
            assertThat(groups.get(1).transactionCount()).isEqualTo(48);
        }

        @Test
        @DisplayName("should return no groups for no descriptions")
        void shouldHandleEmpty() {
            // arrange
            List<TransactionDescriptionDto> rows = List.of();

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(rows, "kroger");

            // assert & verify
            assertThat(groups).isEmpty();
        }
    }

    /** Groups whose keys extend one another by whole words are marked similar, both ways. */
    @Nested
    @DisplayName("similar keys")
    class SimilarKeys {

        @Test
        @DisplayName("should link kroger to both fuel groups and each fuel group back to kroger")
        void shouldMarkLookAlikes() {
            // arrange
            List<TransactionDescriptionDto> rows = KROGER_FIXTURE;

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(rows, null);

            // assert & verify
            assertThat(groups.get(0).similarKeys()).containsExactly("kroger fuel", "kroger fuel ctr");
            assertThat(groups.get(1).similarKeys()).containsExactly("kroger", "kroger fuel");
            assertThat(groups.get(3).similarKeys()).isEmpty();
        }

        @Test
        @DisplayName("should keep the similar marker when the search excludes the look-alike")
        void shouldKeepSimilarAfterExclude() {
            // arrange
            String search = "kroger -fuel";

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(KROGER_FIXTURE, search);

            // assert & verify
            assertThat(groups).extracting(DescriptionGroupDto::key).containsExactly("kroger");
            assertThat(groups.get(0).similarKeys()).containsExactly("kroger fuel", "kroger fuel ctr");
        }
    }

    /** The include phrase and -exclude terms filter variants, then empty groups drop out. */
    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("should filter variants by the include phrase, case-insensitively")
        void shouldIncludeByPhrase() {
            // arrange
            String search = "  Fuel Ctr ";

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(KROGER_FIXTURE, search);

            // assert & verify
            assertThat(groups).extracting(DescriptionGroupDto::key).containsExactly("kroger fuel ctr");
            assertThat(groups.get(0).variantCount()).isEqualTo(2);
            assertThat(groups.get(0).transactionCount()).isEqualTo(48);
        }

        @Test
        @DisplayName("should drop variants containing any exclude term and recount the group")
        void shouldExcludeTerms() {
            // arrange
            String search = "kroger -fuel -roswell";

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(KROGER_FIXTURE, search);

            // assert & verify
            assertThat(groups).hasSize(1);
            assertThat(groups.get(0).variants()).extracting(TransactionDescriptionDto::description)
                    .containsExactly("KROGER #512");
            assertThat(groups.get(0).transactionCount()).isEqualTo(100);
        }

        @Test
        @DisplayName("should treat a lone dash as part of the phrase, not an exclude")
        void shouldIgnoreLoneDash() {
            // arrange
            List<TransactionDescriptionDto> rows = List.of(row("COFFEE - DOWNTOWN #3", 5), row("COFFEE UPTOWN #4", 2));

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(rows, "coffee - downtown");

            // assert & verify
            assertThat(groups).hasSize(1);
            assertThat(groups.get(0).variants()).extracting(TransactionDescriptionDto::description)
                    .containsExactly("COFFEE - DOWNTOWN #3");
        }

        @Test
        @DisplayName("should match include words in any order, around an exclude")
        void shouldMatchIncludeWordsAnywhere() {
            // arrange
            String search = "ctr -sugar kroger";

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(KROGER_FIXTURE, search);

            // assert & verify
            assertThat(groups).hasSize(1);
            assertThat(groups.get(0).variants()).extracting(TransactionDescriptionDto::description)
                    .containsExactly("KROGER FUEL CTR #1431 ROSWELL GA");
        }

        @Test
        @DisplayName("should exclude only words that start with the term")
        void shouldExcludeAtWordStart() {
            // arrange
            List<TransactionDescriptionDto> rows = List.of(row("BIOFUEL DEPOT #2", 3), row("FUELCELL STATION #9", 2), row("GAS N GO #4", 1));

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(rows, "-fuel");

            // assert & verify
            assertThat(groups).extracting(DescriptionGroupDto::key).containsExactly("biofuel depot", "gas n go");
        }

        @Test
        @DisplayName("should allow excludes alone")
        void shouldAllowExcludeOnly() {
            // arrange
            String search = "-kroger";

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(KROGER_FIXTURE, search);

            // assert & verify
            assertThat(groups).extracting(DescriptionGroupDto::key).containsExactly("publix");
        }
    }

    /** Families (same first word) sort by their combined usage, groups within a family by usage. */
    @Nested
    @DisplayName("ordering")
    class Ordering {

        @Test
        @DisplayName("should keep a family together even when another family's group is busier")
        void shouldKeepFamiliesTogether() {
            // arrange
            List<TransactionDescriptionDto> rows = List.of(
                    row("KROGER #1", 50),
                    row("PUBLIX #2", 80),
                    row("KROGER FUEL #3", 40));

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(rows, null);

            // assert & verify
            assertThat(groups).extracting(DescriptionGroupDto::key)
                    .containsExactly("kroger", "kroger fuel", "publix");
        }

        @Test
        @DisplayName("should break ties by key")
        void shouldBreakTiesByKey() {
            // arrange
            List<TransactionDescriptionDto> rows = List.of(row("ZEBRA #1", 5), row("APPLE #1", 5));

            // act
            List<DescriptionGroupDto> groups = DescriptionGrouper.group(rows, null);

            // assert & verify
            assertThat(groups).extracting(DescriptionGroupDto::key).containsExactly("apple", "zebra");
        }
    }
}
