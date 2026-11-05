package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.service.ImportMerchantResolver.ImportRow;
import com.mayureshpatel.pfdataservice.service.categorization.RuleMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link ImportMerchantResolver}'s PF-906 order (exact link, then highest-ranked matching
 * rule, then none), its per-row results, its constant query count, and the description keys it
 * uses, with a mocked {@link MerchantService} and repository and the real {@link RuleMatcher}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImportMerchantResolver Unit Tests")
class ImportMerchantResolverTest {

    private static final Long USER_ID = 1L;
    private static final String KROGER_STORE = "KROGER #123";
    private static final String KROGER = "KROGER";
    private static final String KROGER_NAME = "Kroger";
    private static final String TEN = "10.00";
    private static final String FIVE = "5.00";

    @Mock
    private MerchantService merchantService;
    @Mock
    private MerchantRuleRepository merchantRuleRepository;

    private ImportMerchantResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ImportMerchantResolver(merchantService, merchantRuleRepository, new RuleMatcher());
    }

    private static Merchant merchant(long id, String name) {
        return Merchant.builder().id(id).userId(USER_ID).name(name).build();
    }

    private static MerchantRule rule(long id, Merchant merchant, String... keywords) {
        return MerchantRule.builder().id(id).merchant(merchant).keywords(List.of(keywords))
                .matchType(MatchType.OR).priority(0).build();
    }

    private static ImportRow row(String description, String amount) {
        return new ImportRow(description, new BigDecimal(amount));
    }

    @Test
    @DisplayName("resolveMerchants_linkAndRuleBothMatch_linkWins")
    void resolveMerchants_linkAndRuleBothMatch_linkWins() {
        // arrange
        Merchant linked = merchant(7L, "Linked");
        when(merchantService.findMatchingMerchants(eq(USER_ID), anyCollection())).thenReturn(Map.of(KROGER_STORE, linked));

        // act
        List<Merchant> result = resolver.resolveMerchants(USER_ID, List.of(row(KROGER_STORE, TEN)));

        // assert & verify
        assertThat(result).containsExactly(linked);
        verify(merchantRuleRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("resolveMerchants_noLinkButRuleMatches_ruleMerchantFills")
    void resolveMerchants_noLinkButRuleMatches_ruleMerchantFills() {
        // arrange
        Merchant ruleMerchant = merchant(8L, KROGER_NAME);
        when(merchantService.findMatchingMerchants(eq(USER_ID), anyCollection())).thenReturn(Map.of());
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule(1L, ruleMerchant, KROGER)));

        // act
        List<Merchant> result = resolver.resolveMerchants(USER_ID, List.of(row(KROGER_STORE, TEN)));

        // assert & verify
        assertThat(result).containsExactly(ruleMerchant);
        verify(merchantService, never()).recordDescriptionLink(any(), any(), any());
        verify(merchantService, never()).createMerchant(any(), any());
    }

    @Test
    @DisplayName("resolveMerchants_neitherLinkNorRule_resolvesToNone")
    void resolveMerchants_neitherLinkNorRule_resolvesToNone() {
        // arrange
        when(merchantService.findMatchingMerchants(eq(USER_ID), anyCollection())).thenReturn(Map.of());
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule(1L, merchant(8L, KROGER_NAME), KROGER)));

        // act
        List<Merchant> result = resolver.resolveMerchants(USER_ID, List.of(row("SHELL OIL", TEN)));

        // assert & verify
        assertThat(result).hasSize(1).containsOnlyNulls();
    }

    @Test
    @DisplayName("resolveMerchants_amountRangeRules_pickPerRowForTheSameDescription")
    void resolveMerchants_amountRangeRules_pickPerRowForTheSameDescription() {
        // arrange
        Merchant small = merchant(8L, "Kroger Fuel");
        Merchant large = merchant(9L, "Kroger Grocery");
        MerchantRule smallRule = rule(1L, small, KROGER).toBuilder().maxAmount(new BigDecimal("30")).build();
        MerchantRule largeRule = rule(2L, large, KROGER).toBuilder().minAmount(new BigDecimal("30.01")).build();
        when(merchantService.findMatchingMerchants(eq(USER_ID), anyCollection())).thenReturn(Map.of());
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(smallRule, largeRule));

        // act
        List<Merchant> result = resolver.resolveMerchants(USER_ID,
                List.of(row(KROGER_STORE, "-12.00"), row(KROGER_STORE, "-120.00")));

        // assert & verify
        assertThat(result).containsExactly(small, large);
    }

    @Test
    @DisplayName("resolveMerchants_severalMatchingRules_firstInRankOrderWins")
    void resolveMerchants_severalMatchingRules_firstInRankOrderWins() {
        // arrange
        Merchant best = merchant(8L, "Best");
        Merchant other = merchant(9L, "Other");
        when(merchantService.findMatchingMerchants(eq(USER_ID), anyCollection())).thenReturn(Map.of());
        // the repository returns rules already in rank order, so the first match wins
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule(1L, best, KROGER), rule(2L, other, KROGER)));

        // act
        List<Merchant> result = resolver.resolveMerchants(USER_ID, List.of(row(KROGER_STORE, TEN)));

        // assert & verify
        assertThat(result).containsExactly(best);
    }

    @Test
    @DisplayName("resolveMerchants_ruleWithoutResolvableMerchant_isSkippedForTheNextRule")
    void resolveMerchants_ruleWithoutResolvableMerchant_isSkippedForTheNextRule() {
        // arrange
        Merchant fallback = merchant(9L, "Fallback");
        when(merchantService.findMatchingMerchants(eq(USER_ID), anyCollection())).thenReturn(Map.of());
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(
                rule(1L, null, KROGER),
                rule(2L, Merchant.builder().name("no id").build(), KROGER),
                rule(3L, fallback, KROGER)));

        // act
        List<Merchant> result = resolver.resolveMerchants(USER_ID, List.of(row(KROGER_STORE, TEN)));

        // assert & verify
        assertThat(result).containsExactly(fallback);
    }

    @Test
    @DisplayName("resolveMerchantIds_legacyPath_usesTheSameLinkThenRuleOrder")
    void resolveMerchantIds_legacyPath_usesTheSameLinkThenRuleOrder() {
        // arrange
        when(merchantService.findMatchingMerchantIds(eq(USER_ID), any())).thenReturn(Map.of("LINKED SHOP", 7L));
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule(1L, merchant(8L, KROGER_NAME), KROGER)));

        // act
        List<Long> result = resolver.resolveMerchantIds(USER_ID,
                List.of(row("LINKED SHOP", FIVE), row(KROGER_STORE, FIVE), row("SHELL OIL", FIVE)));

        // assert & verify
        assertThat(result).containsExactly(7L, 8L, null);
    }

    @Test
    @DisplayName("resolve_largeInput_issuesAConstantNumberOfLookups")
    void resolve_largeInput_issuesAConstantNumberOfLookups() {
        // arrange
        when(merchantService.findMatchingMerchants(eq(USER_ID), anyCollection())).thenReturn(Map.of());
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule(1L, merchant(8L, KROGER_NAME), KROGER)));
        List<ImportRow> rows = IntStream.range(0, 5000).mapToObj(i -> row(KROGER + " #" + i, FIVE)).toList();

        // act
        List<Merchant> result = resolver.resolveMerchants(USER_ID, rows);

        // assert & verify
        assertThat(result).hasSize(5000).doesNotContainNull();
        verify(merchantService, times(1)).findMatchingMerchants(eq(USER_ID), anyCollection());
        verify(merchantRuleRepository, times(1)).findByUserId(USER_ID);
    }

    @Test
    @DisplayName("resolve_linkAndRuleKeys_linksGetRawDescriptionsAndRulesMatchTheRawDescription")
    void resolve_linkAndRuleKeys_linksGetRawDescriptionsAndRulesMatchTheRawDescription() {
        // arrange
        // raw text with odd case and spacing: the service folds it for links, so the resolver must
        // pass it through untouched, and the rule matcher sees the same raw text (substring, case-insensitive)
        String raw = "  Kroger   #123  ";
        Merchant ruleMerchant = merchant(8L, KROGER_NAME);
        when(merchantService.findMatchingMerchantIds(eq(USER_ID), any())).thenReturn(Map.of());
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule(1L, ruleMerchant, "kroger   #123")));

        // act
        List<Long> result = resolver.resolveMerchantIds(USER_ID, List.of(row(raw, FIVE)));

        // assert & verify
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> sent = ArgumentCaptor.forClass(List.class);
        verify(merchantService).findMatchingMerchantIds(eq(USER_ID), sent.capture());
        assertThat(sent.getValue()).containsExactly(raw);
        assertThat(result).containsExactly(8L);
    }

    @Test
    @DisplayName("resolve_linkKeyedByFoldedDescription_stillMatchesTheRowThroughTheRawKey")
    void resolve_linkKeyedByFoldedDescription_stillMatchesTheRowThroughTheRawKey() {
        // arrange
        // MerchantService returns links keyed by the RAW description it was given (it folds
        // internally), so a row with odd case and spacing still finds its link by raw text
        String raw = "Starbucks   #1";
        List<String> rawKeys = new ArrayList<>();
        when(merchantService.findMatchingMerchantIds(eq(USER_ID), any())).thenAnswer(invocation -> {
            Collection<String> descriptions = invocation.getArgument(1);
            rawKeys.addAll(descriptions);
            return Map.of(raw, 3L);
        });

        // act
        List<Long> result = resolver.resolveMerchantIds(USER_ID, List.of(row(raw, FIVE)));

        // assert & verify
        assertThat(rawKeys).containsExactly(raw);
        assertThat(result).containsExactly(3L);
        verifyNoInteractions(merchantRuleRepository);
    }

    @Test
    @DisplayName("resolve_emptyRows_issuesNoLookups")
    void resolve_emptyRows_issuesNoLookups() {
        // act
        List<Merchant> result = resolver.resolveMerchants(USER_ID, List.of());

        // assert & verify
        assertThat(result).isEmpty();
        verifyNoInteractions(merchantService, merchantRuleRepository);
    }
}
