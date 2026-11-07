package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.rule.CreateBothAndApplyRequest;
import com.mayureshpatel.pfdataservice.dto.rule.CreateBothAndApplyResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and applies a category rule and a merchant rule together, all or nothing (PF-934).
 *
 * <p>It calls {@link CategoryRuleService#createAndApply} and {@link MerchantRuleService#createAndApply}
 * inside one transaction: each of those joins it instead of starting its own, so if the second fails
 * (a transaction that is not the user's, a merchant that is not theirs, anything unexpected) the
 * first one's rule and row changes, and a merchant created for the second, are rolled back with it.
 */
@Service
@RequiredArgsConstructor
public class RuleApplicationService {

    private final CategoryRuleService categoryRuleService;
    private final MerchantRuleService merchantRuleService;

    /**
     * Creates both rules and applies each to its own chosen transactions, overwriting existing values.
     *
     * @param userId  the signed-in user
     * @param request the category rule and the merchant rule, each with its chosen transactions
     * @return each kind's result, which together are what an undo needs
     */
    @Transactional
    public CreateBothAndApplyResultDto createBothAndApply(Long userId, CreateBothAndApplyRequest request) {
        CategoryRuleApplyResultDto category = categoryRuleService.createAndApply(userId, request.category());
        MerchantRuleApplyResultDto merchant = merchantRuleService.createAndApply(userId, request.merchant());
        return new CreateBothAndApplyResultDto(category, merchant);
    }
}
