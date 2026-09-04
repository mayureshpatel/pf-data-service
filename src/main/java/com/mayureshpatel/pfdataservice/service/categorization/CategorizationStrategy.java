package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

/**
 * A pluggable rule for guessing a transaction's category. {@link TransactionCategorizer} runs
 * every registered strategy in {@link #getOrder()} order and takes the first match; implementing
 * this interface and registering the implementation as a Spring bean is enough to add a new
 * strategy without changing the categorizer itself.
 */
public interface CategorizationStrategy {

    @Getter
    @Builder
    class CategorizationContext {
        private final Long userId;
        private final List<CategoryRule> rules;
        private final List<Category> categories;
    }

    /**
     * Attempts to categorize the transaction.
     *
     * @return Optional containing the category id if matched, or empty if not matched.
     */
    Optional<Long> categorize(Transaction transaction, CategorizationContext context);

    /**
     * Same as {@link #categorize(Transaction, CategorizationContext)}, for a transaction that's
     * being edited rather than one already persisted as a {@link Transaction}.
     *
     * @return Optional containing the category id if matched, or empty if not matched.
     */
    Optional<Long> categorize(TransactionUpdateRequest transaction, CategorizationContext context);

    /**
     * Determines the order in which strategies are applied. Lower values run first.
     */
    int getOrder();
}
