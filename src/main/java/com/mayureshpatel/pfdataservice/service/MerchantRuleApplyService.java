package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.mapper.MerchantDtoMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Previews and applies the user's merchant rules to transactions they already have (PF-907).
 * Both operations share one computation, so what the preview lists is exactly what apply writes.
 *
 * <p>Only transactions with no merchant are ever considered, and a transaction whose description
 * has an exact description link is left alone (an exact link always beats a rule, and assigning the
 * link's merchant is out of scope here). Among matching rules the highest-ranked wins, via
 * {@link ImportMerchantResolver#firstMatchingRule}. Loading is a constant number of queries (rules,
 * candidates, links) and the write is batched, with a {@code merchant_id is null} guard in sql so
 * applying twice, or racing a manual edit, never overwrites a merchant.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantRuleApplyService {

    private final MerchantRuleRepository merchantRuleRepository;
    private final TransactionRepository transactionRepository;
    private final MerchantService merchantService;
    private final ImportMerchantResolver importMerchantResolver;

    /**
     * Lists the changes applying the user's rules would make, without changing anything.
     *
     * @param userId the signed-in user
     * @return one entry per transaction that would get a merchant (possibly empty)
     */
    public List<MerchantRuleChangePreviewDto> preview(Long userId) {
        return computeChanges(userId);
    }

    /**
     * Applies the same changes {@link #preview} lists, in one batch.
     *
     * @param userId the signed-in user
     * @return the number of transactions that actually got a merchant
     */
    @Transactional
    public int apply(Long userId) {
        List<MerchantRuleChangePreviewDto> changes = computeChanges(userId);
        if (changes.isEmpty()) {
            return 0;
        }
        Map<Long, Long> merchantByTransaction = new LinkedHashMap<>();
        for (MerchantRuleChangePreviewDto change : changes) {
            merchantByTransaction.put(change.transactionId(), change.merchant().id());
        }
        return transactionRepository.assignMerchantsWhereMissing(userId, merchantByTransaction);
    }

    /**
     * The single computation behind preview and apply: rules first (no rules, no work), then the
     * merchant-less candidates, then their linked descriptions in one batch.
     */
    private List<MerchantRuleChangePreviewDto> computeChanges(Long userId) {
        List<MerchantRule> rules = merchantRuleRepository.findByUserId(userId);
        if (rules.isEmpty()) {
            return List.of();
        }
        List<Transaction> candidates = transactionRepository.findMerchantlessByUserId(userId);
        if (candidates.isEmpty()) {
            return List.of();
        }
        Map<String, Long> linked = merchantService.findMatchingMerchantIds(userId,
                candidates.stream().map(Transaction::getDescription).toList());

        List<MerchantRuleChangePreviewDto> changes = new ArrayList<>();
        for (Transaction transaction : candidates) {
            if (linked.containsKey(transaction.getDescription())) {
                continue;
            }
            MerchantRule winner = importMerchantResolver.firstMatchingRule(rules,
                    new ImportMerchantResolver.ImportRow(transaction.getDescription(), transaction.getAmount()));
            if (winner != null) {
                changes.add(toChange(transaction, winner));
            }
        }
        return changes;
    }

    /** Maps a transaction and its deciding rule to a preview entry. */
    private static MerchantRuleChangePreviewDto toChange(Transaction transaction, MerchantRule rule) {
        return new MerchantRuleChangePreviewDto(
                transaction.getId(),
                transaction.getTransactionDate(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getAccount() != null ? transaction.getAccount().getName() : null,
                MerchantDtoMapper.toDto(rule.getMerchant()),
                MerchantRuleMatchPreviewService.toRef(rule));
    }
}
