package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Matches a transaction's description against the user's category rules by simple case-insensitive
 * substring containment, in rule order, and takes the first rule that matches. The only
 * {@link CategorizationStrategy} currently registered; runs last among strategies by priority
 * (see {@link #getOrder()}), leaving room for a higher-priority strategy to be added later.
 */
@Component
@Slf4j
public class RuleBasedCategorizationStrategy implements CategorizationStrategy {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Long> categorize(Transaction transaction, CategorizationContext context) {
        // return empty if description or rules are not present
        if (transaction.getDescription() == null || context.getRules() == null) {
            return Optional.empty();
        }

        // search for a matching rule on the transaction
        for (CategoryRule rule : context.getRules()) {
            if (transaction.getDescription().toLowerCase().contains(rule.getKeyword().toLowerCase())) {
                return Optional.of(rule.getCategory().getId());
            }
        }

        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Long> categorize(TransactionUpdateRequest transaction, CategorizationContext context) {
        // return empty if description or rules are not present
        if (transaction.getDescription() == null || context.getRules() == null) {
            return Optional.empty();
        }

        // search for a matching rule on the transaction
        for (CategoryRule rule : context.getRules()) {
            if (transaction.getDescription().toLowerCase().contains(rule.getKeyword().toLowerCase())) {
                return Optional.of(rule.getCategory().getId());
            }
        }

        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
        return 100;
    }
}
