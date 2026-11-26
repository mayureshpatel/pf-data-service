package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.dto.transaction.DescriptionAmountDto;
import com.mayureshpatel.pfdataservice.service.categorization.RuleMatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link MerchantRuleMatchCounter}: per rule, how many distinct descriptions and how
 * many transactions it matches, matching raw text as import does and deduping by folded key.
 * Also records the PF-981 performance measurement for 250 rules over a realistic ledger.
 */
@DisplayName("MerchantRuleMatchCounter Unit Tests")
class MerchantRuleMatchCounterTest {

    private final MerchantRuleMatchCounter counter = new MerchantRuleMatchCounter(new RuleMatcher());

    private static DescriptionAmountDto row(String description, String amount) {
        return new DescriptionAmountDto(description, new BigDecimal(amount));
    }

    private static MerchantRule rule(long id, BigDecimal min, BigDecimal max, String... keywords) {
        return MerchantRule.builder().id(id).priority(0).matchType(MatchType.OR)
                .keywords(List.of(keywords)).minAmount(min).maxAmount(max).build();
    }

    @Test
    @DisplayName("should count transactions and distinct folded descriptions, whether or not the rule wins")
    void shouldCountMatches() {
        // arrange
        List<DescriptionAmountDto> rows = List.of(
                row("KROGER #1", "-10.00"),
                row("kroger #1", "-12.00"),
                row("KROGER #2", "-5.00"),
                row("KROGER FUEL #3", "-40.00"),
                row("PUBLIX", "-9.00"));
        MerchantRule kroger = rule(1L, null, null, "kroger");
        MerchantRule fuel = rule(2L, null, null, "kroger fuel");

        // act
        Map<Long, MerchantRuleMatchCounter.Counts> counts = counter.count(List.of(kroger, fuel), rows);

        // assert & verify
        assertThat(counts.get(1L)).isEqualTo(new MerchantRuleMatchCounter.Counts(3, 4));
        assertThat(counts.get(2L)).isEqualTo(new MerchantRuleMatchCounter.Counts(1, 1));
    }

    @Test
    @DisplayName("should apply the amount range to the absolute amount and give unmatched rules zero")
    void shouldApplyAmountRange() {
        // arrange
        List<DescriptionAmountDto> rows = List.of(row("SHELL 1", "-30.00"), row("SHELL 2", "-80.00"));
        MerchantRule small = rule(1L, new BigDecimal("10"), new BigDecimal("50"), "shell");
        MerchantRule none = rule(2L, null, null, "chevron");

        // act
        Map<Long, MerchantRuleMatchCounter.Counts> counts = counter.count(List.of(small, none), rows);

        // assert & verify
        assertThat(counts.get(1L)).isEqualTo(new MerchantRuleMatchCounter.Counts(1, 1));
        assertThat(counts.get(2L)).isEqualTo(new MerchantRuleMatchCounter.Counts(0, 0));
    }

    @Test
    @DisplayName("should count 250 rules over 1,000 descriptions and 20,000 transactions")
    void shouldCountLargeLedger() {
        // arrange -- the pf-981 size check; measured at about 0.4 s on a dev laptop, recorded on the ticket, not asserted
        List<MerchantRule> rules = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            rules.add(rule(i, null, null, "merchant" + i + " "));
        }
        List<DescriptionAmountDto> rows = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) {
            int description = i % 1_000;
            rows.add(row("MERCHANT" + (description % 300) + " STORE " + description + " ROSWELL GA", "-" + (i % 200 + 1) + ".00"));
        }

        // act
        Map<Long, MerchantRuleMatchCounter.Counts> counts = counter.count(rules, rows);

        // assert & verify
        assertThat(counts).hasSize(250);
        // merchant7 is in descriptions 7, 307, 607 and 907, each used 20 times
        assertThat(counts.get(7L)).isEqualTo(new MerchantRuleMatchCounter.Counts(4, 80));
        // merchant249 is in descriptions 249, 549 and 849
        assertThat(counts.get(249L)).isEqualTo(new MerchantRuleMatchCounter.Counts(3, 60));
    }
}
