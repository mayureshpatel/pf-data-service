package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.category.AppliedCategoryChangeDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.AppliedMerchantChangeDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.rule.RuleApplicationUndoRequest;
import com.mayureshpatel.pfdataservice.dto.rule.RuleApplicationUndoResultDto;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Undoes a rule the ledger just created and applied (PF-922): puts each changed transaction's
 * previous category and/or merchant back and deletes the rule(s) the apply created, as one
 * all-or-nothing step.
 *
 * <p>The request is the create-and-apply result sent back as it was, which carries the value the
 * apply set. A transaction is restored only if it still has that value, checked here and again in
 * the update's own {@code where} clause, so a row changed since (by the owner, an import or anything
 * else) is left alone and reported rather than overwritten. Everything is scoped to the user: a
 * transaction that is not theirs is reported as skipped, and a rule that is not theirs, or that no
 * longer exists, is simply not deleted (the response says whether each rule was deleted).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RuleApplicationUndoService {

    private final CategoryRuleRepository categoryRuleRepository;
    private final MerchantRuleRepository merchantRuleRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Undoes the given create-and-apply result(s).
     *
     * @param userId  the signed-in user
     * @param request the category and/or merchant result to undo
     * @return how many distinct transactions were restored, which were skipped, and which rules were deleted
     */
    @Transactional
    public RuleApplicationUndoResultDto undo(Long userId, RuleApplicationUndoRequest request) {
        Set<Long> restored = new LinkedHashSet<>();
        Set<Long> skipped = new LinkedHashSet<>();
        boolean categoryRuleDeleted = request.category() != null && undoCategory(userId, request.category(), restored, skipped);
        boolean merchantRuleDeleted = request.merchant() != null && undoMerchant(userId, request.merchant(), restored, skipped);

        // a row put back for one kind of rule is not "skipped" just because the other kind left it alone
        skipped.removeAll(restored);
        return new RuleApplicationUndoResultDto(restored.size(), List.copyOf(skipped), categoryRuleDeleted, merchantRuleDeleted);
    }

    /** Restores the categories an apply changed, then deletes its rule; true if the rule was deleted. */
    private boolean undoCategory(Long userId, CategoryRuleApplyResultDto result, Set<Long> restored, Set<Long> skipped) {
        List<AppliedCategoryChangeDto> changes = result.changes() == null ? List.of() : result.changes();
        Map<Long, Transaction> current = loadOwned(userId, changes.stream().map(AppliedCategoryChangeDto::transactionId).toList());

        Map<Long, List<Long>> idsByPreviousCategory = new LinkedHashMap<>();
        for (AppliedCategoryChangeDto change : changes) {
            Transaction transaction = current.get(change.transactionId());
            boolean untouchedSince = transaction != null && transaction.getCategory() != null
                    && transaction.getCategory().getId().equals(result.categoryId());
            if (untouchedSince) {
                idsByPreviousCategory.computeIfAbsent(change.previousCategoryId(), key -> new ArrayList<>()).add(change.transactionId());
                restored.add(change.transactionId());
            } else {
                skipped.add(change.transactionId());
            }
        }
        idsByPreviousCategory.forEach((previous, ids) ->
                transactionRepository.restoreCategory(userId, result.categoryId(), previous, ids));
        return deleteOwnedCategoryRule(userId, result.ruleId());
    }

    /** Restores the merchants an apply changed, then deletes its rule; true if the rule was deleted. */
    private boolean undoMerchant(Long userId, MerchantRuleApplyResultDto result, Set<Long> restored, Set<Long> skipped) {
        List<AppliedMerchantChangeDto> changes = result.changes() == null ? List.of() : result.changes();
        Map<Long, Transaction> current = loadOwned(userId, changes.stream().map(AppliedMerchantChangeDto::transactionId).toList());

        Map<Long, List<Long>> idsByPreviousMerchant = new LinkedHashMap<>();
        for (AppliedMerchantChangeDto change : changes) {
            Transaction transaction = current.get(change.transactionId());
            boolean untouchedSince = transaction != null && transaction.getMerchant() != null
                    && transaction.getMerchant().getId().equals(result.merchantId());
            if (untouchedSince) {
                idsByPreviousMerchant.computeIfAbsent(change.previousMerchantId(), key -> new ArrayList<>()).add(change.transactionId());
                restored.add(change.transactionId());
            } else {
                skipped.add(change.transactionId());
            }
        }
        idsByPreviousMerchant.forEach((previous, ids) ->
                transactionRepository.restoreMerchant(userId, result.merchantId(), previous, ids));
        return deleteOwnedMerchantRule(userId, result.ruleId());
    }

    /** The user's own transactions among the ids, by id; ids that are not theirs are absent. */
    private Map<Long, Transaction> loadOwned(Long userId, List<Long> ids) {
        return transactionRepository.findAllById(userId, ids).stream()
                .collect(Collectors.toMap(Transaction::getId, Function.identity()));
    }

    /** Deletes the category rule if it exists and is the user's (the delete is user-scoped in sql); false otherwise. */
    private boolean deleteOwnedCategoryRule(Long userId, Long ruleId) {
        return ruleId != null && categoryRuleRepository.deleteById(ruleId, userId) > 0;
    }

    /** Deletes the merchant rule if it exists and is the user's (the delete is user-scoped in sql); false otherwise. */
    private boolean deleteOwnedMerchantRule(Long userId, Long ruleId) {
        return ruleId != null && merchantRuleRepository.deleteById(ruleId, userId) > 0;
    }
}
