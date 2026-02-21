package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class RuleBasedCategorizationStrategy implements CategorizationStrategy {

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

    @Override
    public int getOrder() {
        return 100;
    }
}
