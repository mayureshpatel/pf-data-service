package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionRuleCoverageDto;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.RuleMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link TransactionRuleCoverageService} (PF-919): which kind of saved rule covers each
 * transaction, the amount range, ownership scoping, an empty request, and that the number of
 * repository calls stays constant however many ids are asked about -- with mocked repositories
 * and the real {@link RuleMatcher}, so the answer uses the same matching as applying rules.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionRuleCoverageService Unit Tests")
class TransactionRuleCoverageServiceTest {

    private static final Long USER_ID = 1L;
    private static final String TACO_BELL = "TACO BELL";
    private static final String TACO_BELL_DESCRIPTION = "TACO BELL 041246 ROSWELL GA";
    private static final String TARGET_DESCRIPTION = "TARGET T-1892";
    private static final String TEN = "10.00";

    @Mock
    private CategoryRuleRepository categoryRuleRepository;
    @Mock
    private MerchantRuleRepository merchantRuleRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private TransactionRuleCoverageService service;

    @BeforeEach
    void setUp() {
        service = new TransactionRuleCoverageService(
                categoryRuleRepository, merchantRuleRepository, transactionRepository, new RuleMatcher());
    }

    private static CategoryRule categoryRule(long id, String keyword) {
        return CategoryRule.builder().id(id).keywords(List.of(keyword)).matchType(MatchType.OR).priority(0).build();
    }

    private static MerchantRule merchantRule(long id, String keyword) {
        return MerchantRule.builder().id(id).keywords(List.of(keyword)).matchType(MatchType.OR).priority(0).build();
    }

    private static Transaction transaction(long id, String description, String amount) {
        return Transaction.builder().id(id).description(description).amount(new BigDecimal(amount)).build();
    }

    @Test
    @DisplayName("should report a category-rule match only")
    void findCoverage_categoryOnly() {
        // arrange
        when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(categoryRule(1, TACO_BELL)));
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(transactionRepository.findAllById(USER_ID, List.of(7L)))
                .thenReturn(List.of(transaction(7, TACO_BELL_DESCRIPTION, TEN)));

        // act
        List<TransactionRuleCoverageDto> result = service.findCoverage(USER_ID, List.of(7L));

        // assert & verify
        assertThat(result).containsExactly(new TransactionRuleCoverageDto(7L, true, false));
    }

    @Test
    @DisplayName("should report a merchant-rule match only")
    void findCoverage_merchantOnly() {
        // arrange
        when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(merchantRule(1, "target")));
        when(transactionRepository.findAllById(USER_ID, List.of(8L)))
                .thenReturn(List.of(transaction(8, TARGET_DESCRIPTION, TEN)));

        // act
        List<TransactionRuleCoverageDto> result = service.findCoverage(USER_ID, List.of(8L));

        // assert & verify -- the keyword matches ignoring case
        assertThat(result).containsExactly(new TransactionRuleCoverageDto(8L, false, true));
    }

    @Test
    @DisplayName("should report both kinds when both match, and neither when nothing matches")
    void findCoverage_bothAndNeither() {
        // arrange
        when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(categoryRule(1, TACO_BELL)));
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(merchantRule(2, TACO_BELL)));
        when(transactionRepository.findAllById(USER_ID, List.of(1L, 2L))).thenReturn(List.of(
                transaction(1, TACO_BELL_DESCRIPTION, TEN), transaction(2, TARGET_DESCRIPTION, TEN)));

        // act
        List<TransactionRuleCoverageDto> result = service.findCoverage(USER_ID, List.of(1L, 2L));

        // assert & verify
        assertThat(result).containsExactlyInAnyOrder(
                new TransactionRuleCoverageDto(1L, true, true),
                new TransactionRuleCoverageDto(2L, false, false));
    }

    @Test
    @DisplayName("should not count a rule whose amount range excludes the transaction")
    void findCoverage_amountRangeNotMatching() {
        // arrange
        CategoryRule underFive = CategoryRule.builder().id(1L).keywords(List.of(TACO_BELL))
                .matchType(MatchType.OR).priority(0).maxAmount(new BigDecimal("5.00")).build();
        when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(underFive));
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(transactionRepository.findAllById(USER_ID, List.of(3L)))
                .thenReturn(List.of(transaction(3, TACO_BELL_DESCRIPTION, TEN)));

        // act
        List<TransactionRuleCoverageDto> result = service.findCoverage(USER_ID, List.of(3L));

        // assert & verify
        assertThat(result).containsExactly(new TransactionRuleCoverageDto(3L, false, false));
    }

    @Test
    @DisplayName("should omit an id the user does not own")
    void findCoverage_foreignIdOmitted() {
        // arrange -- the user-scoped lookup silently skips an id that is not theirs
        when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(categoryRule(1, TACO_BELL)));
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(transactionRepository.findAllById(USER_ID, List.of(1L, 99L)))
                .thenReturn(List.of(transaction(1, TACO_BELL_DESCRIPTION, TEN)));

        // act
        List<TransactionRuleCoverageDto> result = service.findCoverage(USER_ID, List.of(1L, 99L));

        // assert & verify
        assertThat(result).extracting(TransactionRuleCoverageDto::transactionId).containsExactly(1L);
    }

    @Test
    @DisplayName("should return nothing and touch no repository for an empty list")
    void findCoverage_emptyList() {
        // act
        List<TransactionRuleCoverageDto> result = service.findCoverage(USER_ID, List.of());

        // assert & verify
        assertThat(result).isEmpty();
        verifyNoInteractions(categoryRuleRepository, merchantRuleRepository, transactionRepository);
    }

    @Test
    @DisplayName("should load the rules and the transactions once, however many ids are asked about")
    void findCoverage_constantRepositoryCalls() {
        // arrange
        List<Long> ids = IntStream.rangeClosed(1, 50).mapToObj(Long::valueOf).toList();
        List<Transaction> transactions = ids.stream().map(id -> transaction(id, TACO_BELL_DESCRIPTION, TEN)).toList();
        when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(categoryRule(1, TACO_BELL)));
        when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(merchantRule(2, TACO_BELL)));
        when(transactionRepository.findAllById(USER_ID, ids)).thenReturn(transactions);

        // act
        List<TransactionRuleCoverageDto> result = service.findCoverage(USER_ID, ids);

        // assert & verify
        assertThat(result).hasSize(50);
        verify(categoryRuleRepository, times(1)).findByUserId(USER_ID);
        verify(merchantRuleRepository, times(1)).findByUserId(USER_ID);
        verify(transactionRepository, times(1)).findAllById(USER_ID, ids);
        verify(transactionRepository, times(0)).findByUserId(USER_ID);
    }
}
