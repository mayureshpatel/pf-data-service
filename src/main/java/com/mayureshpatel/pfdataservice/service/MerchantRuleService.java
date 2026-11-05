package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.MerchantRuleDtoMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD for keyword-based merchant rules (PF-904): keywords that point at one of the user's
 * merchants. The target merchant must belong to the signed-in user (a foreign or missing merchant
 * is a 404, checked through {@link MerchantService#requireOwnedMerchants}), and the owner always
 * comes from the session, never the request. Matching and applying rules are separate tickets.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantRuleService {

    private static final String RULE_NOT_FOUND = "Rule not found";

    private final MerchantRuleRepository merchantRuleRepository;
    private final UserRepository userRepository;
    private final MerchantService merchantService;

    /**
     * Get all merchant rules for a user, in rule priority order
     *
     * @param userId the user id
     * @return the list of {@link MerchantRuleDto}
     */
    public List<MerchantRuleDto> getRules(Long userId) {
        return merchantRuleRepository.findByUserId(userId).stream()
                .map(MerchantRuleDtoMapper::toDto)
                .toList();
    }

    /**
     * Create a new merchant rule for a user
     *
     * @param userId  the user id
     * @param request the merchant rule create request
     * @return the newly created rule's generated id
     * @throws ResourceNotFoundException if the merchant doesn't exist or isn't the user's
     */
    @Transactional
    public Long createRule(Long userId, MerchantRuleCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        merchantService.requireOwnedMerchants(userId, List.of(request.getMerchantId()));

        MerchantRule rule = MerchantRule.builder()
                .user(user)
                .keywords(request.getKeywords())
                .matchType(request.getMatchType() != null ? request.getMatchType() : MatchType.OR)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .merchant(Merchant.builder().id(request.getMerchantId()).build())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .audit(TableAudit.insertAudit(user))
                .build();

        return merchantRuleRepository.insertAndReturnId(rule);
    }

    /**
     * Update an existing merchant rule for a user
     *
     * @param userId  the user id
     * @param request the updated merchant rule request
     * @return the number of rows updated
     * @throws ResourceNotFoundException if the rule or the merchant doesn't exist or isn't the user's
     */
    @Transactional
    public int updateRule(Long userId, MerchantRuleUpdateRequest request) {
        MerchantRule rule = requireOwnedRule(userId, request.getId());

        merchantService.requireOwnedMerchants(userId, List.of(request.getMerchantId()));

        MerchantRule updatedRule = rule.toBuilder()
                .keywords(request.getKeywords())
                .matchType(request.getMatchType() != null ? request.getMatchType() : MatchType.OR)
                .merchant(Merchant.builder().id(request.getMerchantId()).build())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .audit(TableAudit.updateAudit(rule.getUser()))
                .build();

        return merchantRuleRepository.update(updatedRule);
    }

    /**
     * Delete an existing merchant rule for a user
     *
     * @param userId the user id
     * @param ruleId the rule id to delete
     */
    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        requireOwnedRule(userId, ruleId);
        merchantRuleRepository.deleteById(ruleId, userId);
    }

    /**
     * Loads a rule and confirms it belongs to the user, as category rules do.
     */
    private MerchantRule requireOwnedRule(Long userId, Long ruleId) {
        MerchantRule rule = merchantRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException(RULE_NOT_FOUND));

        if (!rule.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this rule");
        }
        return rule;
    }
}
