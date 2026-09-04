package com.mayureshpatel.pfdataservice.service.transfer;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
import com.mayureshpatel.pfdataservice.mapper.TransactionDtoMapper;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds transaction pairs that look like a transfer between two of a user's own accounts: equal
 * amount, opposite type, different account, within a 3-day window. Purely a heuristic -- it
 * suggests candidates for the user to confirm via {@code TransactionService#markAsTransfer}, it
 * doesn't mark anything itself. Only ever matches a transaction into one pair; it doesn't detect
 * a single transfer that was split across multiple transactions on the other side.
 */
@Component
public class TransferMatcher {

    /**
     * Finds candidate transfer pairs among a set of transactions. Each transaction is matched at
     * most once, to whichever earlier-considered candidate it pairs with first.
     *
     * @param transactions the transactions to search for pairs within, any order
     * @return the candidate transfer pairs found, with a confidence score that decreases the
     * further apart the two transactions' dates are
     */
    public List<TransferSuggestionDto> findMatches(List<Transaction> transactions) {
        // the inner loop's early break below is only correct if transactions are ordered by
        // date - it assumes daysDiff is non-decreasing as j increases. the real caller's query
        // happens to sort that way, but nothing enforced it, so unsorted input silently dropped
        // valid matches. sort defensively instead of relying on the caller's contract.
        List<Transaction> sorted = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate))
                .toList();

        List<TransferSuggestionDto> suggestions = new ArrayList<>();
        Set<Long> matchedIds = new HashSet<>();

        for (int i = 0; i < sorted.size(); i++) {
            Transaction t1 = sorted.get(i);
            if (matchedIds.contains(t1.getId())) continue;

            for (int j = i + 1; j < sorted.size(); j++) {
                Transaction t2 = sorted.get(j);
                if (matchedIds.contains(t2.getId())) continue;

                long daysDiff = Math.abs(ChronoUnit.DAYS.between(t1.getTransactionDate(), t2.getTransactionDate()));

                if (daysDiff > 3) {
                    break;
                }

                if (t1.getAmount().compareTo(t2.getAmount()) == 0) {
                    if (t1.getType() != t2.getType()) {
                        if (!t1.getAccount().getId().equals(t2.getAccount().getId())) {
                            suggestions.add(new TransferSuggestionDto(
                                    TransactionDtoMapper.toDto(t1),
                                    TransactionDtoMapper.toDto(t2),
                                    0.9 - (daysDiff * 0.1)
                            ));

                            matchedIds.add(t1.getId());
                            matchedIds.add(t2.getId());
                            break;
                        }
                    }
                }
            }
        }
        return suggestions;
    }
}
