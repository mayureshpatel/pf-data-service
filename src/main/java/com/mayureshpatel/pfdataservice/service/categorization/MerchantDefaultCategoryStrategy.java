package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Falls back to a merchant's recency-weighted default category (PF-815) when no
 * {@link CategoryRule} matches -- proves out the {@link CategorizationStrategy} extension point
 * for a genuinely independent strategy, rather than another condition bolted onto
 * {@link RuleBasedCategorizationStrategy}. Runs at a lower priority (see {@link #getOrder()}) than
 * the rule-based strategy, so an explicit keyword/amount rule always wins when one matches; this
 * only ever fires when {@link RuleBasedCategorizationStrategy} found nothing.
 * <p>
 * The default itself is never stored -- {@link TransactionRepository#findWeightedDefaultCategoryId}
 * recomputes it live from the merchant's transaction history every time, which is deliberate: the
 * decay weighting is relative to "now," so a cached value would go stale daily even with zero new
 * transactions, before a single new transaction ever arrived to invalidate it.
 */
@Component
@RequiredArgsConstructor
public class MerchantDefaultCategoryStrategy implements CategorizationStrategy {

    /** A transaction this old contributes half the weight of a brand-new one toward its category's score. */
    private static final long HALF_LIFE_SECONDS = 180L * 24 * 60 * 60;

    /** A category needs at least this many of the merchant's transactions before it's trusted as the default. */
    private static final int MIN_OCCURRENCES = 2;

    private final TransactionRepository transactionRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Long> categorize(Transaction transaction, CategorizationContext context) {
        if (transaction.getMerchant() == null || context.getUserId() == null) {
            return Optional.empty();
        }
        return transactionRepository.findWeightedDefaultCategoryId(
                transaction.getMerchant().getId(), context.getUserId(), HALF_LIFE_SECONDS, MIN_OCCURRENCES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Long> categorize(TransactionUpdateRequest transaction, CategorizationContext context) {
        if (transaction.getMerchantId() == null || context.getUserId() == null) {
            return Optional.empty();
        }
        return transactionRepository.findWeightedDefaultCategoryId(
                transaction.getMerchantId(), context.getUserId(), HALF_LIFE_SECONDS, MIN_OCCURRENCES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
        return 200;
    }
}
