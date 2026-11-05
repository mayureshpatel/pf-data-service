package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.RuleMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link MerchantRuleApplyService} (PF-907): which merchant-less transactions get a
 * merchant, the exclusion of linked descriptions, rule ranking and amount ranges, the shared
 * computation behind preview and apply, the real affected count, the constant query count and the
 * user scoping, with mocked repositories and the real {@link RuleMatcher} and resolver.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantRuleApplyService Unit Tests")
class MerchantRuleApplyServiceTest {

    private static final Long USER_ID = 1L;
    private static final String KROGER_STORE = "KROGER #123";
    private static final String KROGER = "KROGER";
    private static final String KROGER_NAME = "Kroger";
    private static final String TEN = "10.00";
    private static final String FIVE = "5.00";

    @Mock
    private MerchantRuleRepository merchantRuleRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private MerchantService merchantService;

    private MerchantRuleApplyService service;

    @BeforeEach
    void setUp() {
        ImportMerchantResolver resolver = new ImportMerchantResolver(merchantService, merchantRuleRepository, new RuleMatcher());
        service = new MerchantRuleApplyService(merchantRuleRepository, transactionRepository, merchantService, resolver);
    }

    private static Merchant merchant(long id, String name) {
        return Merchant.builder().id(id).userId(USER_ID).name(name).build();
    }

    private static MerchantRule rule(long id, Merchant merchant, String keyword) {
        return MerchantRule.builder().id(id).merchant(merchant).keywords(List.of(keyword))
                .matchType(MatchType.OR).priority(0).build();
    }

    private static Transaction transaction(long id, String description, String amount) {
        return Transaction.builder().id(id).description(description).amount(new BigDecimal(amount))
                .account(Account.builder().name("Main").build()).build();
    }

    @Test
    @DisplayName("preview_ruleMatchesMerchantlessTransaction_listsChangeWithMerchantAndRule")
    void preview_ruleMatchesMerchantlessTransaction_listsChangeWithMerchantAndRule() {
        // arrange
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule(9L, merchant(7L, KROGER_NAME), KROGER)));
        when(transactionRepository.findMerchantlessByUserId(USER_ID)).thenReturn(List.of(transaction(1L, KROGER_STORE, TEN)));
        when(merchantService.findMatchingMerchantIds(eq(USER_ID), anyList())).thenReturn(Map.of());

        // act
        List<MerchantRuleChangePreviewDto> result = service.preview(USER_ID);

        // assert & verify
        assertThat(result).hasSize(1);
        MerchantRuleChangePreviewDto change = result.get(0);
        assertThat(change.transactionId()).isEqualTo(1L);
        assertThat(change.accountName()).isEqualTo("Main");
        assertThat(change.merchant().id()).isEqualTo(7L);
        assertThat(change.merchant().name()).isEqualTo(KROGER_NAME);
        assertThat(change.rule().id()).isEqualTo(9L);
        verify(transactionRepository, never()).assignMerchantsWhereMissing(any(), anyMap());
    }

    @Test
    @DisplayName("preview_descriptionHasExactLink_isLeftAlone")
    void preview_descriptionHasExactLink_isLeftAlone() {
        // arrange
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule(9L, merchant(7L, KROGER_NAME), KROGER)));
        when(transactionRepository.findMerchantlessByUserId(USER_ID)).thenReturn(List.of(transaction(1L, KROGER_STORE, TEN)));
        when(merchantService.findMatchingMerchantIds(eq(USER_ID), anyList())).thenReturn(Map.of(KROGER_STORE, 3L));

        // act
        List<MerchantRuleChangePreviewDto> result = service.preview(USER_ID);
        int applied = service.apply(USER_ID);

        // assert & verify
        assertThat(result).isEmpty();
        assertThat(applied).isZero();
        verify(transactionRepository, never()).assignMerchantsWhereMissing(any(), anyMap());
    }

    @Test
    @DisplayName("preview_highestRankedMatchingRuleWins")
    void preview_highestRankedMatchingRuleWins() {
        // arrange -- the repository returns rules already in rank order, first match wins
        MerchantRule first = rule(1L, merchant(7L, KROGER_NAME), KROGER);
        MerchantRule second = rule(2L, merchant(8L, "Other"), "KRO");
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(first, second));
        when(transactionRepository.findMerchantlessByUserId(USER_ID)).thenReturn(List.of(transaction(1L, KROGER_STORE, TEN)));
        when(merchantService.findMatchingMerchantIds(eq(USER_ID), anyList())).thenReturn(Map.of());

        // act
        List<MerchantRuleChangePreviewDto> result = service.preview(USER_ID);

        // assert & verify
        assertThat(result).hasSize(1);
        assertThat(result.get(0).merchant().id()).isEqualTo(7L);
        assertThat(result.get(0).rule().id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("preview_amountRangeRule_respectedPerTransaction")
    void preview_amountRangeRule_respectedPerTransaction() {
        // arrange
        MerchantRule ranged = rule(1L, merchant(7L, KROGER_NAME), KROGER).toBuilder()
                .minAmount(new BigDecimal("50.00")).maxAmount(new BigDecimal("100.00")).build();
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(ranged));
        when(transactionRepository.findMerchantlessByUserId(USER_ID)).thenReturn(List.of(
                transaction(1L, KROGER_STORE, TEN), transaction(2L, KROGER_STORE, "75.00")));
        when(merchantService.findMatchingMerchantIds(eq(USER_ID), anyList())).thenReturn(Map.of());

        // act
        List<MerchantRuleChangePreviewDto> result = service.preview(USER_ID);

        // assert & verify
        assertThat(result).extracting(MerchantRuleChangePreviewDto::transactionId).containsExactly(2L);
    }

    @Test
    @DisplayName("preview_noRules_doesNotLoadTransactions")
    void preview_noRules_doesNotLoadTransactions() {
        // arrange
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());

        // act
        List<MerchantRuleChangePreviewDto> result = service.preview(USER_ID);

        // assert & verify
        assertThat(result).isEmpty();
        verifyNoInteractions(transactionRepository, merchantService);
    }

    @Test
    @DisplayName("apply_noCandidates_returnsZeroWithoutWriting")
    void apply_noCandidates_returnsZeroWithoutWriting() {
        // arrange -- the idempotent second run: everything already has a merchant
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule(9L, merchant(7L, KROGER_NAME), KROGER)));
        when(transactionRepository.findMerchantlessByUserId(USER_ID)).thenReturn(List.of());

        // act
        int result = service.apply(USER_ID);

        // assert & verify
        assertThat(result).isZero();
        verify(transactionRepository, never()).assignMerchantsWhereMissing(any(), anyMap());
        verifyNoInteractions(merchantService);
    }

    @Test
    @DisplayName("apply_writesExactlyThePreviewedChangesAndReturnsRepositoryCount")
    void apply_writesExactlyThePreviewedChangesAndReturnsRepositoryCount() {
        // arrange -- the repository reports fewer rows than sent, as when one row raced a manual edit
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule(9L, merchant(7L, KROGER_NAME), KROGER)));
        when(transactionRepository.findMerchantlessByUserId(USER_ID)).thenReturn(List.of(
                transaction(1L, KROGER_STORE, TEN), transaction(2L, "KROGER #9", FIVE), transaction(3L, "SHELL", FIVE)));
        when(merchantService.findMatchingMerchantIds(eq(USER_ID), anyList())).thenReturn(Map.of());
        when(transactionRepository.assignMerchantsWhereMissing(eq(USER_ID), anyMap())).thenReturn(1);

        // act
        int result = service.apply(USER_ID);

        // assert & verify
        assertThat(result).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, Long>> captor = ArgumentCaptor.forClass(Map.class);
        verify(transactionRepository).assignMerchantsWhereMissing(eq(USER_ID), captor.capture());
        assertThat(captor.getValue()).containsOnly(Map.entry(1L, 7L), Map.entry(2L, 7L));
    }

    @Test
    @DisplayName("apply_loadsEverythingOnceAndScopesToTheUser")
    void apply_loadsEverythingOnceAndScopesToTheUser() {
        // arrange
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule(9L, merchant(7L, KROGER_NAME), KROGER)));
        when(transactionRepository.findMerchantlessByUserId(USER_ID)).thenReturn(List.of(
                transaction(1L, KROGER_STORE, TEN), transaction(2L, "KROGER #9", FIVE)));
        when(merchantService.findMatchingMerchantIds(eq(USER_ID), anyList())).thenReturn(Map.of());
        when(transactionRepository.assignMerchantsWhereMissing(eq(USER_ID), anyMap())).thenReturn(2);

        // act
        service.apply(USER_ID);

        // assert & verify -- one load per kind and one batch write, whatever the number of rows
        verify(merchantRuleRepository, times(1)).findByUserId(USER_ID);
        verify(transactionRepository, times(1)).findMerchantlessByUserId(USER_ID);
        verify(merchantService, times(1)).findMatchingMerchantIds(eq(USER_ID), anyList());
        verify(transactionRepository, times(1)).assignMerchantsWhereMissing(eq(USER_ID), anyMap());
    }
}
