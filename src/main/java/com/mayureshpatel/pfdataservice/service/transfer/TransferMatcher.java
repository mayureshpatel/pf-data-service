package com.mayureshpatel.pfdataservice.service.transfer;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
import com.mayureshpatel.pfdataservice.mapper.TransactionDtoMapper;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Finds transaction pairs that look like a transfer between two of a user's own accounts: equal
 * amount, opposite type, different account, within a 3-day window; or, up to 7 days apart, when the
 * two rows also look like the same transfer (both filed under a Transfers category, or one on a credit
 * card account, whose payments often take days to post). Purely a heuristic -- it
 * suggests candidates for the user to confirm via {@code TransactionService#markAsTransfer}, it
 * doesn't mark anything itself. Only ever matches a transaction into one pair; it doesn't detect
 * a single transfer that was split across multiple transactions on the other side.
 */
@Component
public class TransferMatcher {

    private static final int MAX_DAYS_APART_FOR_TRANSFER_MATCH = 3;
    private static final int MAX_DAYS_APART_FOR_RELATED_TRANSFER_MATCH = 7;
    private static final String TRANSFERS_CATEGORY_NAME = "Transfers";
    private static final String CREDIT_CARD_ACCOUNT_TYPE_CODE = "CREDIT_CARD";

    /**
     * Finds candidate transfer pairs among a set of transactions. Each transaction is matched at
     * most once, to the candidate closest in date to it.
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

            findMatchForTransaction(sorted, i, t1, matchedIds).ifPresent(suggestions::add);
        }
        return suggestions;
    }

    /**
     * Searches forward from just after {@code t1}'s own position for the transaction that pairs with it as
     * a transfer candidate and is closest in date, marking both ids matched if found. {@code sorted} is
     * date-ordered, so the search ends at the first gap beyond the widest window. A candidate more than
     * 3 days away only counts when the two rows look like the same transfer (PF-938).
     *
     * @return the suggestion, or empty if nothing within the window matched
     */
    private Optional<TransferSuggestionDto> findMatchForTransaction(List<Transaction> sorted, int t1Index, Transaction t1, Set<Long> matchedIds) {
        Transaction best = null;
        long bestDaysDiff = Long.MAX_VALUE;

        for (int j = t1Index + 1; j < sorted.size(); j++) {
            Transaction t2 = sorted.get(j);
            if (matchedIds.contains(t2.getId())) continue;

            long daysDiff = Math.abs(ChronoUnit.DAYS.between(t1.getTransactionDate(), t2.getTransactionDate()));

            if (daysDiff > MAX_DAYS_APART_FOR_RELATED_TRANSFER_MATCH) {
                break;
            }

            // a candidate is only ever kept if it is strictly closer than the best so far
            if (daysDiff < bestDaysDiff && isTransferMatch(t1, t2)
                    && (daysDiff <= MAX_DAYS_APART_FOR_TRANSFER_MATCH || looksLikeSameTransfer(t1, t2))) {
                best = t2;
                bestDaysDiff = daysDiff;
            }
        }

        if (best == null) {
            return Optional.empty();
        }
        matchedIds.add(t1.getId());
        matchedIds.add(best.getId());
        return Optional.of(new TransferSuggestionDto(
                TransactionDtoMapper.toDto(t1),
                TransactionDtoMapper.toDto(best),
                0.9 - (bestDaysDiff * 0.1)
        ));
    }

    /**
     * Whether two rows already look like the two sides of one transfer, which is what allows them to be
     * paired across a longer gap: both are filed under a Transfers category (directly or as the parent), or
     * at least one is on a credit card account.
     */
    private boolean looksLikeSameTransfer(Transaction t1, Transaction t2) {
        return (inTransfersCategory(t1) && inTransfersCategory(t2))
                || isCreditCard(t1) || isCreditCard(t2);
    }

    private boolean inTransfersCategory(Transaction t) {
        Category category = t.getCategory();
        if (category == null) return false;
        return TRANSFERS_CATEGORY_NAME.equalsIgnoreCase(category.getName())
                || (category.getParent() != null && TRANSFERS_CATEGORY_NAME.equalsIgnoreCase(category.getParent().getName()));
    }

    private boolean isCreditCard(Transaction t) {
        AccountType type = t.getAccount().getType();
        return type != null && CREDIT_CARD_ACCOUNT_TYPE_CODE.equals(type.getCode());
    }

    private boolean isTransferMatch(Transaction t1, Transaction t2) {
        return t1.getAmount().compareTo(t2.getAmount()) == 0
                && t1.getType() != t2.getType()
                && !t1.getAccount().getId().equals(t2.getAccount().getId());
    }
}
