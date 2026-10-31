package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantDescriptionLinkRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.DismissedPatternRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies {@code RecurringSuggestionFinder}'s pattern-detection -- groups expense history by
 * merchant/description and proposes a recurring pattern wherever the intervals between
 * occurrences are stable enough to classify into one of {@link Frequency}'s buckets (weekly
 * through yearly, including a PF-205 quarterly bucket); an unstable interval, too few grouped
 * transactions, or an interval average that falls in a genuinely unclassified gap between buckets
 * all correctly produce no suggestion for that group, and null names/descriptions/merchants are
 * tolerated without throwing. A PF-834 fix: a single subscription whose price changed partway
 * through its history (e.g. a Netflix plan upgrade) merges into exactly one suggestion reflecting
 * the current price and the full occurrence count, not one conflicting suggestion per price tier.
 * A PF-835 fix: the reported confidence score is a genuine 0-100 percentage that never exceeds
 * 100, not the old formula's un-rescaled raw value that could read above 1.0 without being a real
 * percentage. (PF-809: moved here from RecurringTransactionServiceTest when the pattern-detection
 * logic itself moved to its own class.)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecurringSuggestionFinder Unit Tests")
class RecurringSuggestionFinderTest {

    @Mock private RecurringTransactionRepository recurringRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private DismissedPatternRepository dismissedRepository;
    @Mock private MerchantDescriptionLinkRepository linkRepository;

    @InjectMocks private RecurringSuggestionFinder suggestionFinder;

    private static final Long USER_ID = 1L;
    private static final String MERCHANT_NETFLIX = "Netflix";
    private static final String LATEST_AMOUNT = "15.49";
    private static final String METHOD_CALCULATE_NEXT_DATE = "calculateNextDate";

    /**
     * {@code findSuggestions} groups expense history by merchant/description and proposes a
     * recurring pattern wherever the intervals between occurrences are stable enough to classify
     * into one of {@link Frequency}'s buckets (weekly through yearly, including a PF-205 quarterly
     * bucket); an unstable interval, too few grouped transactions, or an interval average that
     * falls in a genuinely unclassified gap between buckets all correctly produce no suggestion for
     * that group, and null names/descriptions/merchants are tolerated without throwing. A PF-834
     * fix: a single subscription whose price changed partway through its history (e.g. a Netflix
     * plan upgrade) merges into exactly one suggestion reflecting the current price and the full
     * occurrence count, not one conflicting suggestion per price tier. A PF-835 fix: the reported
     * confidence score is a genuine 0-100 percentage that never exceeds 100, not the old formula's
     * un-rescaled raw value that could read above 1.0 without being a real percentage.
     */
    @Nested
    @DisplayName("findSuggestions")
    class FindSuggestionsTests {

        @Test
        @DisplayName("should detect various stable intervals (Weekly, Monthly, Bi-Weekly, Quarterly, Yearly)")
        void shouldDetectAllFrequencies() {
            // arrange
            LocalDate now = LocalDate.now();
            OffsetDateTime zone = OffsetDateTime.now();

            // Weekly (avg ~7)
            Transaction w1 = Transaction.builder().transactionDate(now.minusDays(14).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("W").build();
            Transaction w2 = Transaction.builder().transactionDate(now.minusDays(7).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("W").build();
            Transaction w3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("W").build();

            // Monthly (avg ~30)
            Transaction m1 = Transaction.builder().transactionDate(now.minusMonths(2).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ONE).description("M").build();
            Transaction m2 = Transaction.builder().transactionDate(now.minusMonths(1).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ONE).description("M").build();
            Transaction m3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ONE).description("M").build();

            // Bi-Weekly (avg ~14)
            Transaction b1 = Transaction.builder().transactionDate(now.minusWeeks(4).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ZERO).description("B").build();
            Transaction b2 = Transaction.builder().transactionDate(now.minusWeeks(2).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ZERO).description("B").build();
            Transaction b3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ZERO).description("B").build();

            // Quarterly (avg ~90) -- PF-205
            Transaction q1 = Transaction.builder().transactionDate(now.minusDays(180).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.valueOf(150)).description("Q").build();
            Transaction q2 = Transaction.builder().transactionDate(now.minusDays(90).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.valueOf(150)).description("Q").build();
            Transaction q3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.valueOf(150)).description("Q").build();

            // Yearly (avg ~365)
            Transaction y1 = Transaction.builder().transactionDate(now.minusDays(730).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("Y").build();
            Transaction y2 = Transaction.builder().transactionDate(now.minusDays(365).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("Y").build();
            Transaction y3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("Y").build();

            when(recurringRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(List.of(w1, w2, w3, m1, m2, m3, b1, b2, b3, q1, q2, q3, y1, y2, y3));

            // act
            List<RecurringSuggestionDto> result = suggestionFinder.findSuggestions(USER_ID);

            // assert & verify
            assertTrue(result.stream().anyMatch(s -> s.frequency() == Frequency.WEEKLY));
            assertTrue(result.stream().anyMatch(s -> s.frequency() == Frequency.MONTHLY));
            assertTrue(result.stream().anyMatch(s -> s.frequency() == Frequency.BI_WEEKLY));
            assertTrue(result.stream().anyMatch(s -> s.frequency() == Frequency.QUARTERLY));
            assertTrue(result.stream().anyMatch(s -> s.frequency() == Frequency.YEARLY));
        }

        @Test
        @DisplayName("should return empty if intervals are unstable")
        void shouldReturnEmptyForUnstableIntervals() {
            // arrange
            LocalDate now = LocalDate.now();
            Transaction t1 = Transaction.builder().transactionDate(now.minusDays(40).atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("R").build();
            Transaction t2 = Transaction.builder().transactionDate(now.minusDays(20).atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("R").build();
            Transaction t3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("R").build();

            when(recurringRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(List.of(t1, t2, t3));

            // act
            List<RecurringSuggestionDto> result = suggestionFinder.findSuggestions(USER_ID);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should handle null names and empty descriptions and null merchants")
        void shouldHandleNullNames() {
            // arrange
            Transaction t1 = Transaction.builder().description(null).merchant(null).build();
            Transaction t2 = Transaction.builder().description("  ").merchant(null).build();
            Merchant m = Merchant.builder().name(null).build();
            Transaction t3 = Transaction.builder().description(null).merchant(m).build();
            Transaction t4 = Transaction.builder().description("D").merchant(null).build();

            when(recurringRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(List.of(t1, t2, t3, t4, t4, t4));

            // act
            List<RecurringSuggestionDto> result = suggestionFinder.findSuggestions(USER_ID);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return null if intervals match no known frequency")
        void shouldHandleNoMatchedFrequency() {
            // arrange
            LocalDate now = LocalDate.now();
            // Avg interval ~50 (no frequency for this)
            Transaction t1 = Transaction.builder().transactionDate(now.minusDays(100).atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("None").build();
            Transaction t2 = Transaction.builder().transactionDate(now.minusDays(50).atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("None").build();
            Transaction t3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("None").build();

            when(recurringRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(List.of(t1, t2, t3));

            // act
            List<RecurringSuggestionDto> result = suggestionFinder.findSuggestions(USER_ID);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("bug regression: a subscription that changed price mid-history must produce one "
                + "suggestion, not one per price tier (PF-834) -- confirmed live, Netflix's real 56-month "
                + "history across 4 price tiers surfaced as multiple simultaneous, conflicting suggestions")
        void shouldMergePriceTiersIntoOneSuggestion() {
            // arrange -- same merchant, monthly cadence throughout, price increases partway through
            LocalDate now = LocalDate.now();
            OffsetDateTime offset = OffsetDateTime.now();
            List<Transaction> netflix = List.of(
                    Transaction.builder().transactionDate(now.minusMonths(5).atStartOfDay().atOffset(offset.getOffset())).amount(new BigDecimal("13.99")).description(MERCHANT_NETFLIX).build(),
                    Transaction.builder().transactionDate(now.minusMonths(4).atStartOfDay().atOffset(offset.getOffset())).amount(new BigDecimal("13.99")).description(MERCHANT_NETFLIX).build(),
                    Transaction.builder().transactionDate(now.minusMonths(3).atStartOfDay().atOffset(offset.getOffset())).amount(new BigDecimal("13.99")).description(MERCHANT_NETFLIX).build(),
                    Transaction.builder().transactionDate(now.minusMonths(2).atStartOfDay().atOffset(offset.getOffset())).amount(new BigDecimal(LATEST_AMOUNT)).description(MERCHANT_NETFLIX).build(),
                    Transaction.builder().transactionDate(now.minusMonths(1).atStartOfDay().atOffset(offset.getOffset())).amount(new BigDecimal(LATEST_AMOUNT)).description(MERCHANT_NETFLIX).build(),
                    Transaction.builder().transactionDate(now.atStartOfDay().atOffset(offset.getOffset())).amount(new BigDecimal(LATEST_AMOUNT)).description(MERCHANT_NETFLIX).build()
            );

            when(recurringRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(netflix);

            // act
            List<RecurringSuggestionDto> result = suggestionFinder.findSuggestions(USER_ID);

            // assert & verify -- one suggestion, reflecting the current price and the full history
            long netflixSuggestions = result.stream().filter(s -> MERCHANT_NETFLIX.equals(s.merchant().name())).count();
            assertEquals(1, netflixSuggestions, "expected exactly one Netflix suggestion, got: " + result);

            RecurringSuggestionDto suggestion = result.stream()
                    .filter(s -> MERCHANT_NETFLIX.equals(s.merchant().name())).findFirst().orElseThrow();
            assertEquals(0, new BigDecimal(LATEST_AMOUNT).compareTo(suggestion.amount()), "expected the current/most-recent price");
            assertEquals(6, suggestion.occurrenceCount(), "expected the full history counted, not just the current tier");
            assertEquals(Frequency.MONTHLY, suggestion.frequency());
        }

        @Test
        @DisplayName("PF-835: confidence score is a real 0-100 percentage, never exceeding 100")
        void confidenceScoreStaysWithinPercentageBounds() {
            // arrange -- 12 monthly occurrences, a full year's worth within the lookback window
            LocalDate now = LocalDate.now();
            OffsetDateTime offset = OffsetDateTime.now();
            List<Transaction> group = new java.util.ArrayList<>();
            for (int i = 11; i >= 0; i--) {
                group.add(Transaction.builder()
                        .transactionDate(now.minusMonths(i).atStartOfDay().atOffset(offset.getOffset()))
                        .amount(BigDecimal.TEN)
                        .description("LongRunning")
                        .build());
            }

            when(recurringRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(group);

            // act
            List<RecurringSuggestionDto> result = suggestionFinder.findSuggestions(USER_ID);

            // assert & verify -- a strong, 12-occurrence pattern should read as a real, high
            // percentage (well above the un-rescaled raw score of ~1.4 the old formula produced,
            // which technically satisfies ">1.0" but isn't a percentage by any reasonable reading)
            assertEquals(1, result.size());
            double score = result.get(0).confidenceScore();
            assertTrue(score >= 50.0, "expected a real percentage for a strong pattern (>=50), got: " + score);
            assertTrue(score <= 100.0, "confidence must never exceed 100%, got: " + score);
        }
    }

    /** The private {@code detectFrequency} helper (invoked here via reflection) classifies a stable ~90-day interval as QUARTERLY (PF-205), and correctly returns null -- not the nearest bucket -- for a stable interval that falls in a deliberately unclassified gap between two buckets (e.g. ~20 days, between BI_WEEKLY's and MONTHLY's ranges). */
    @Nested
    @DisplayName("detectFrequency")
    class DetectFrequencyTests {
        @Test
        @DisplayName("should detect QUARTERLY for a stable ~90-day interval group (PF-205)")
        void shouldDetectQuarterly() {
            // arrange
            OffsetDateTime zone = OffsetDateTime.now();
            LocalDate now = LocalDate.now();
            List<Transaction> group = List.of(
                    Transaction.builder().transactionDate(now.minusDays(180).atStartOfDay().atOffset(zone.getOffset())).build(),
                    Transaction.builder().transactionDate(now.minusDays(90).atStartOfDay().atOffset(zone.getOffset())).build(),
                    Transaction.builder().transactionDate(now.atStartOfDay().atOffset(zone.getOffset())).build()
            );

            // act
            Frequency result = FrequencyDetector.detect(group);

            // assert & verify
            assertEquals(Frequency.QUARTERLY, result);
        }

        @Test
        @DisplayName("should still return null for a stable interval in an intentionally-unclassified gap (e.g. ~20 days) (PF-205)")
        void shouldReturnNullForIntentionallyUnclassifiedGap() {
            // arrange -- ~20 days falls between BI_WEEKLY's (13-16) and MONTHLY's (25-35) buckets;
            // no Frequency enum value corresponds to a ~20-day cadence, so this must stay null
            OffsetDateTime zone = OffsetDateTime.now();
            LocalDate now = LocalDate.now();
            List<Transaction> group = List.of(
                    Transaction.builder().transactionDate(now.minusDays(40).atStartOfDay().atOffset(zone.getOffset())).build(),
                    Transaction.builder().transactionDate(now.minusDays(20).atStartOfDay().atOffset(zone.getOffset())).build(),
                    Transaction.builder().transactionDate(now.atStartOfDay().atOffset(zone.getOffset())).build()
            );

            // act
            Frequency result = FrequencyDetector.detect(group);

            // assert & verify
            assertNull(result);
        }
    }

    /** The private {@code calculateNextDate} helper (invoked here via reflection) advances a given date by each {@link Frequency}'s own real calendar increment -- a calendar month/week/quarter/year, not a fixed day count. */
    @Nested
    @DisplayName(METHOD_CALCULATE_NEXT_DATE)
    class CalculateNextDateTests {
        @Test
        @DisplayName("should calculate correct next dates for all frequencies via reflection")
        void shouldCalculateCorrectly() {
            // arrange
            LocalDate last = LocalDate.of(2026, 3, 1);

            // act & assert & verify
            assertEquals(last.plusMonths(1), ReflectionTestUtils.invokeMethod(suggestionFinder, METHOD_CALCULATE_NEXT_DATE, last, Frequency.MONTHLY));
            assertEquals(last.plusWeeks(1), ReflectionTestUtils.invokeMethod(suggestionFinder, METHOD_CALCULATE_NEXT_DATE, last, Frequency.WEEKLY));
            assertEquals(last.plusWeeks(2), ReflectionTestUtils.invokeMethod(suggestionFinder, METHOD_CALCULATE_NEXT_DATE, last, Frequency.BI_WEEKLY));
            assertEquals(last.plusMonths(3), ReflectionTestUtils.invokeMethod(suggestionFinder, METHOD_CALCULATE_NEXT_DATE, last, Frequency.QUARTERLY));
            assertEquals(last.plusYears(1), ReflectionTestUtils.invokeMethod(suggestionFinder, METHOD_CALCULATE_NEXT_DATE, last, Frequency.YEARLY));
        }
    }

    /**
     * A PF-816 mutation-testing gap: PiTest's ConditionalsBoundaryMutator flipped each of
     * {@code detectFrequency}'s five range comparisons' {@code >=}/{@code <=} boundaries and every
     * flip still passed the existing suite, since nothing exercised an average interval landing
     * exactly on a boundary. Since {@code detectFrequency} is private, these go through the public
     * {@code findSuggestions} entry point with a constructed transaction group whose intervals
     * average out to each exact boundary value, parameterized across all 10 boundary points (both
     * ends of all 5 frequency ranges) plus the nearest achievable value just outside each one.
     */
    @Nested
    @DisplayName("detectFrequency boundary values (PF-816)")
    class DetectFrequencyBoundaryTests {
        // PF-816: PiTest's ConditionalsBoundaryMutator flipped each of these 5 ranges' >=/<=
        // boundaries and every flip still passed the existing suite -- no test exercised an
        // avgInterval landing exactly on a boundary. detectFrequency is private, so these go
        // through the public findSuggestions entry point (per this class's own established
        // pattern, e.g. shouldMergePriceTiersIntoOneSuggestion) with a constructed transaction
        // group whose intervals average out to each exact value, not a reflection-based call.
        //
        // "On boundary" cases use constant intervals so avgInterval equals the boundary exactly.
        // "Just outside" cases nudge one interval by a day so the average lands at the finest
        // achievable distance past the boundary using a small (3-interval) group -- day-level
        // interval granularity means an exact "8.01" isn't constructible without an impractically
        // large fixture; "one interval off" is the practical equivalent for this class's own
        // interval-averaging arithmetic.
        private static Stream<Arguments> boundaryCases() {
            return Stream.of(
                    Arguments.of("WEEKLY lower bound (6)", List.of(6L, 6L, 6L), Frequency.WEEKLY),
                    Arguments.of("WEEKLY upper bound (8)", List.of(8L, 8L, 8L), Frequency.WEEKLY),
                    Arguments.of("BI_WEEKLY lower bound (13)", List.of(13L, 13L, 13L), Frequency.BI_WEEKLY),
                    Arguments.of("BI_WEEKLY upper bound (16)", List.of(16L, 16L, 16L), Frequency.BI_WEEKLY),
                    Arguments.of("MONTHLY lower bound (25)", List.of(25L, 25L, 25L), Frequency.MONTHLY),
                    Arguments.of("MONTHLY upper bound (35)", List.of(35L, 35L, 35L), Frequency.MONTHLY),
                    Arguments.of("QUARTERLY lower bound (85)", List.of(85L, 85L, 85L), Frequency.QUARTERLY),
                    Arguments.of("QUARTERLY upper bound (95)", List.of(95L, 95L, 95L), Frequency.QUARTERLY),
                    Arguments.of("YEARLY lower bound (360)", List.of(360L, 360L, 360L), Frequency.YEARLY),
                    Arguments.of("YEARLY upper bound (370)", List.of(370L, 370L, 370L), Frequency.YEARLY),

                    Arguments.of("just below WEEKLY lower bound (avg 5.67)", List.of(6L, 6L, 5L), null),
                    Arguments.of("just above WEEKLY upper bound (avg 8.33)", List.of(8L, 8L, 9L), null),
                    Arguments.of("just below BI_WEEKLY lower bound (avg 12.67)", List.of(13L, 13L, 12L), null),
                    Arguments.of("just above BI_WEEKLY upper bound (avg 16.33)", List.of(16L, 16L, 17L), null),
                    Arguments.of("just below MONTHLY lower bound (avg 24.67)", List.of(25L, 25L, 24L), null),
                    Arguments.of("just above MONTHLY upper bound (avg 35.33)", List.of(35L, 35L, 36L), null),
                    Arguments.of("just below QUARTERLY lower bound (avg 84.67)", List.of(85L, 85L, 84L), null),
                    Arguments.of("just above QUARTERLY upper bound (avg 95.33)", List.of(95L, 95L, 96L), null),
                    Arguments.of("just below YEARLY lower bound (avg 359.67)", List.of(360L, 360L, 359L), null),
                    Arguments.of("just above YEARLY upper bound (avg 370.33)", List.of(370L, 370L, 371L), null),

                    // PF-855: a separate boundary on the SAME method -- the "stable enough to call
                    // recurring at all" check (Math.abs(i - avgInterval) < 5), distinct from the 5
                    // named frequency ranges above. Both cases hold avgInterval at exactly 30 (the
                    // MONTHLY bucket's midpoint) so classifyFrequency never confounds the result --
                    // only the single most-deviant interval's distance from that average changes.
                    Arguments.of("stability boundary: largest deviation exactly 5 (unstable)", List.of(35L, 27L, 28L), null),
                    Arguments.of("stability boundary: largest deviation 4, just under 5 (stable)", List.of(34L, 28L, 28L), Frequency.MONTHLY)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("boundaryCases")
        void shouldClassifyAtBoundary(String caseName, List<Long> intervalDays, Frequency expected) {
            // arrange
            when(recurringRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(groupWithIntervals(intervalDays));

            // act
            List<RecurringSuggestionDto> result = suggestionFinder.findSuggestions(USER_ID);

            // assert & verify
            if (expected == null) {
                assertTrue(result.isEmpty(), caseName + ": expected no suggestion, got: " + result);
            } else {
                assertEquals(1, result.size(), caseName);
                assertEquals(expected, result.get(0).frequency(), caseName);
            }
        }

        private List<Transaction> groupWithIntervals(List<Long> intervalDays) {
            OffsetDateTime offset = OffsetDateTime.now();
            long totalSpan = intervalDays.stream().mapToLong(Long::longValue).sum();
            LocalDate cursor = LocalDate.now().minusDays(totalSpan);

            List<Transaction> group = new ArrayList<>();
            group.add(Transaction.builder()
                    .transactionDate(cursor.atStartOfDay().atOffset(offset.getOffset()))
                    .amount(BigDecimal.TEN)
                    .description("PF-816 Boundary Test Vendor")
                    .build());
            for (Long gap : intervalDays) {
                cursor = cursor.plusDays(gap);
                group.add(Transaction.builder()
                        .transactionDate(cursor.atStartOfDay().atOffset(offset.getOffset()))
                        .amount(BigDecimal.TEN)
                        .description("PF-816 Boundary Test Vendor")
                        .build());
            }
            return group;
        }
    }

    /** PF-884: detection finds yearly charges seen twice, judges stability against each frequency's own tolerance, treats a price change as one pattern while reporting the previous price, and reports the account and a folded pattern key. */
    @Nested
    @DisplayName("PF-884 detection upgrades")
    class DetectionUpgradeTests {

        private static final String VENDOR = "Vendor";
        private static final String STEADY_AMOUNT = "10.00";

        private Transaction tx(LocalDate date, String amount, String description, Account account) {
            return Transaction.builder()
                    .transactionDate(date.atStartOfDay().atOffset(ZoneOffset.UTC))
                    .amount(new BigDecimal(amount))
                    .description(description)
                    .account(account)
                    .build();
        }

        /** Builds a group whose consecutive gaps are exactly {@code gaps}, ending today. */
        private List<Transaction> withGaps(String description, long... gaps) {
            long span = 0;
            for (long gap : gaps) {
                span += gap;
            }
            LocalDate cursor = LocalDate.now().minusDays(span);
            List<Transaction> group = new ArrayList<>();
            group.add(tx(cursor, STEADY_AMOUNT, description, null));
            for (long gap : gaps) {
                cursor = cursor.plusDays(gap);
                group.add(tx(cursor, STEADY_AMOUNT, description, null));
            }
            return group;
        }

        /** Like {@link #withGaps} but the newest charge was {@code endedDaysAgo} days ago. */
        private List<Transaction> withGapsEndingAgo(String description, long endedDaysAgo, long... gaps) {
            long span = 0;
            for (long gap : gaps) {
                span += gap;
            }
            LocalDate cursor = LocalDate.now().minusDays(endedDaysAgo + span);
            List<Transaction> group = new ArrayList<>();
            group.add(tx(cursor, STEADY_AMOUNT, description, null));
            for (long gap : gaps) {
                cursor = cursor.plusDays(gap);
                group.add(tx(cursor, STEADY_AMOUNT, description, null));
            }
            return group;
        }

        private static long[] repeat(long gap, int times) {
            long[] gaps = new long[times];
            java.util.Arrays.fill(gaps, gap);
            return gaps;
        }

        private List<RecurringSuggestionDto> find(List<Transaction> transactions) {
            when(recurringRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(transactions);
            return suggestionFinder.findSuggestions(USER_ID);
        }

        @Test
        @DisplayName("should exclude a dismissed pattern and look the keys up for this user only")
        void shouldExcludeDismissedPattern() {
            // arrange
            when(dismissedRepository.findKeysByUserId(USER_ID)).thenReturn(java.util.Set.of("vendor"));

            // act
            List<RecurringSuggestionDto> result = find(withGaps(VENDOR, 30, 30));

            // assert & verify
            assertTrue(result.isEmpty());
            verify(dismissedRepository).findKeysByUserId(USER_ID);
        }

        @Test
        @DisplayName("should bring a pattern back once its dismissal is gone")
        void shouldSuggestAgainAfterRestore() {
            // arrange
            when(dismissedRepository.findKeysByUserId(USER_ID)).thenReturn(java.util.Set.of());

            // act & assert & verify
            assertEquals(1, find(withGaps(VENDOR, 30, 30)).size());
        }

        @Test
        @DisplayName("should exclude a pattern that matches a tracked merchant's linked description")
        void shouldExcludeLinkedDescription() {
            // arrange
            RecurringTransaction tracked = RecurringTransaction.builder()
                    .merchant(Merchant.builder().id(7L).name("Streaming Co").build())
                    .build();
            when(recurringRepository.findAllByUserId(USER_ID)).thenReturn(List.of(tracked));
            when(linkRepository.findNormalizedDescriptionsByMerchantIds(USER_ID, java.util.Set.of(7L)))
                    .thenReturn(java.util.Set.of("netflix.com 866"));
            when(transactionRepository.findExpensesSince(eq(USER_ID), any()))
                    .thenReturn(withGaps("NETFLIX.COM  866", 30, 30));

            // act
            List<RecurringSuggestionDto> result = suggestionFinder.findSuggestions(USER_ID);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should still exclude a pattern named like a paused tracked merchant")
        void shouldExcludePausedTrackedMerchant() {
            // arrange
            RecurringTransaction paused = RecurringTransaction.builder()
                    .active(false)
                    .merchant(Merchant.builder().id(7L).name(VENDOR).build())
                    .build();
            when(recurringRepository.findAllByUserId(USER_ID)).thenReturn(List.of(paused));
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(withGaps(VENDOR, 30, 30));

            // act & assert & verify
            assertTrue(suggestionFinder.findSuggestions(USER_ID).isEmpty());
        }

        @Test
        @DisplayName("should not suggest a pattern whose last charge is more than two periods ago (a cancelled subscription)")
        void shouldNotSuggestCancelledSubscriptions() {
            // act & assert & verify
            assertTrue(find(withGapsEndingAgo(VENDOR, 800, repeat(30, 11))).isEmpty());
        }

        @Test
        @DisplayName("should still suggest a pattern whose last charge is a little late")
        void shouldSuggestALittleLatePattern() {
            // act & assert & verify
            assertEquals(1, find(withGapsEndingAgo(VENDOR, 40, 30, 30, 30)).size());
        }

        @Test
        @DisplayName("should still find a long monthly pattern with one missed month")
        void shouldSurviveOneMissedMonth() {
            // arrange -- 24 months with a 61-day gap where a card expired
            long[] gaps = new long[24];
            java.util.Arrays.fill(gaps, 30);
            gaps[11] = 61;

            // act
            List<RecurringSuggestionDto> result = find(withGaps(VENDOR, gaps));

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(Frequency.MONTHLY, result.get(0).frequency());
        }

        @Test
        @DisplayName("should still reject a long group when many gaps stray")
        void shouldRejectWhenManyGapsStray() {
            // arrange -- 3 of 10 gaps are far off the 30-day rhythm
            long[] gaps = {30, 30, 30, 61, 30, 30, 61, 30, 30, 61};

            // act & assert & verify
            assertTrue(find(withGaps(VENDOR, gaps)).isEmpty());
        }

        @Test
        @DisplayName("should tolerate a monthly charge slipping 4 days over a weekend")
        void shouldTolerateMonthlyWeekendSlip() {
            // act & assert & verify
            assertEquals(Frequency.MONTHLY, find(withGaps(VENDOR, 34, 28, 30)).get(0).frequency());
        }

        @Test
        @DisplayName("should report the merchant a pattern's description is already linked to")
        void shouldReportLinkedMerchant() {
            // arrange
            com.mayureshpatel.pfdataservice.domain.merchant.Merchant linked =
                    Merchant.builder().id(7L).name("Streaming Co").build();
            when(linkRepository.findMerchantsByNormalizedDescriptions(eq(USER_ID), any()))
                    .thenReturn(java.util.Map.of("vendor", linked));

            // act
            RecurringSuggestionDto suggestion = find(withGaps(VENDOR, 30, 30)).get(0);

            // assert & verify
            assertEquals(7L, suggestion.linkedMerchantId());
            assertEquals("Streaming Co", suggestion.linkedMerchantName());
        }

        @Test
        @DisplayName("should leave the linked merchant empty when the description is not linked")
        void shouldLeaveLinkedMerchantEmpty() {
            // act
            RecurringSuggestionDto suggestion = find(withGaps(VENDOR, 30, 30)).get(0);

            // assert & verify
            assertNull(suggestion.linkedMerchantId());
            assertNull(suggestion.linkedMerchantName());
        }

        @Test
        @DisplayName("should find a yearly charge seen twice about a year apart")
        void shouldFindYearlyWithTwoSightings() {
            // act
            List<RecurringSuggestionDto> result = find(withGaps(VENDOR, 365));

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(Frequency.YEARLY, result.get(0).frequency());
            assertEquals(2, result.get(0).occurrenceCount());
        }

        @Test
        @DisplayName("should not call a two-sighting monthly group a pattern")
        void shouldNeedThreeSightingsForMonthly() {
            // act & assert & verify
            assertTrue(find(withGaps(VENDOR, 30)).isEmpty());
        }

        @Test
        @DisplayName("should ask the lookback for about three years of expenses")
        void shouldLookBackThreeYears() {
            // act
            find(Collections.emptyList());

            // assert & verify
            org.mockito.ArgumentCaptor<LocalDate> since = org.mockito.ArgumentCaptor.forClass(LocalDate.class);
            verify(transactionRepository).findExpensesSince(eq(USER_ID), since.capture());
            assertEquals(LocalDate.now().minusYears(3), since.getValue());
        }

        @Test
        @DisplayName("should accept a quarterly group whose gaps vary by 9 days")
        void shouldToleratePeriodicSlipForQuarterly() {
            // act & assert & verify
            assertEquals(Frequency.QUARTERLY, find(withGaps(VENDOR, 81, 99, 90)).get(0).frequency());
        }

        @Test
        @DisplayName("should accept a yearly group whose gaps vary by 7 days")
        void shouldToleratePeriodicSlipForYearly() {
            // act & assert & verify
            assertEquals(Frequency.YEARLY, find(withGaps(VENDOR, 358, 372)).get(0).frequency());
        }

        @Test
        @DisplayName("should reject a weekly group with a 3-day slip")
        void shouldBeStrictForWeekly() {
            // act & assert & verify
            assertTrue(find(withGaps(VENDOR, 4, 10, 7)).isEmpty());
        }

        @Test
        @DisplayName("should reject a monthly group with a 12-day slip")
        void shouldRejectLargeMonthlySlip() {
            // act & assert & verify
            assertTrue(find(withGaps(VENDOR, 42, 30, 18)).isEmpty());
        }

        @Test
        @DisplayName("should report the previous amount and the date the price changed")
        void shouldReportPriceChange() {
            // arrange
            LocalDate now = LocalDate.now();
            List<Transaction> group = List.of(
                    tx(now.minusMonths(3), STEADY_AMOUNT, MERCHANT_NETFLIX, null),
                    tx(now.minusMonths(2), STEADY_AMOUNT, MERCHANT_NETFLIX, null),
                    tx(now.minusMonths(1), "12.00", MERCHANT_NETFLIX, null),
                    tx(now, "12.00", MERCHANT_NETFLIX, null));

            // act
            RecurringSuggestionDto suggestion = find(group).get(0);

            // assert & verify
            assertEquals(0, new BigDecimal("12.00").compareTo(suggestion.amount()));
            assertEquals(0, new BigDecimal(STEADY_AMOUNT).compareTo(suggestion.previousAmount()));
            assertEquals(now.minusMonths(1), suggestion.priceChangedOn());
        }

        @Test
        @DisplayName("should leave previousAmount and priceChangedOn null when the price never changed")
        void shouldLeavePriceChangeNullWhenSteady() {
            // act
            RecurringSuggestionDto suggestion = find(withGaps(VENDOR, 30, 30)).get(0);

            // assert & verify
            assertNull(suggestion.previousAmount());
            assertNull(suggestion.priceChangedOn());
        }

        @Test
        @DisplayName("should report the account of the most recent charge")
        void shouldReportMostRecentAccount() {
            // arrange
            LocalDate now = LocalDate.now();
            Account old = Account.builder().id(1L).name("Old Card").build();
            Account current = Account.builder().id(2L).name("New Card").build();
            List<Transaction> group = List.of(
                    tx(now.minusDays(60), STEADY_AMOUNT, VENDOR, old),
                    tx(now.minusDays(30), STEADY_AMOUNT, VENDOR, old),
                    tx(now, STEADY_AMOUNT, VENDOR, current));

            // act
            RecurringSuggestionDto suggestion = find(group).get(0);

            // assert & verify
            assertEquals(2L, suggestion.accountId());
            assertEquals("New Card", suggestion.accountName());
        }

        @Test
        @DisplayName("should fold case and spacing in the pattern key and group spellings together")
        void shouldFoldThePatternKey() {
            // arrange
            LocalDate now = LocalDate.now();
            List<Transaction> group = List.of(
                    tx(now.minusDays(60), STEADY_AMOUNT, "Net  Flix", null),
                    tx(now.minusDays(30), STEADY_AMOUNT, "net flix ", null),
                    tx(now, STEADY_AMOUNT, "Net  Flix", null));

            // act
            List<RecurringSuggestionDto> result = find(group);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("net flix", result.get(0).patternKey());
            assertEquals(3, result.get(0).occurrenceCount());
            assertEquals("Net  Flix", result.get(0).merchant().name());
        }
    }
}
