package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.merchant.AppliedMerchantChangeDto;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyRule;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyResultDto;
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
    private final TransactionRepository transactionRepository;

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
    /** The rule's existing merchant, or the one named for it, created now if it does not exist yet. */
    private Long resolveMerchant(Long userId, MerchantRuleApplyRule rule) {
        if (rule.merchantId() != null) {
            return rule.merchantId();
        }
        return merchantService.findOrCreateByName(userId, rule.newMerchantName());
    }

    private MerchantRule requireOwnedRule(Long userId, Long ruleId) {
        MerchantRule rule = merchantRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException(RULE_NOT_FOUND));

        if (!rule.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this rule");
        }
        return rule;
    }

    /**
     * PF-921: creates a merchant rule and applies it, in one all-or-nothing step, to the
     * transactions the user chose, overwriting any merchant they have. Only the chosen
     * transactions change (never anything else), and a transaction that already has the rule's
     * merchant is left alone. Every transaction that did change is returned with its previous
     * merchant so the change can be undone. Settings merchant-rule apply and import are untouched:
     * they still only fill merchant-less rows.
     *
     * <p>The merchant is an existing one of the user's or a new name, created (or an existing one with
     * that name reused) inside the same transaction (PF-933), so a failed save leaves no stray merchant.
     * Every chosen id must be the user's and the merchant must be theirs; the rule is created
     * (which checks the merchant) before the update, and the whole method is one transaction, so a
     * failure part-way, after the rule was saved, rolls the rule back too.
     *
     * @param userId  the user id
     * @param request the rule to create and the chosen transaction ids
     * @return the new rule id and the changed transactions with their previous merchants
     * @throws AccessDeniedException if a chosen transaction is not the user's
     */
    @Transactional
    public MerchantRuleApplyResultDto createAndApply(Long userId, MerchantRuleApplyRequest request) {
        List<Long> ids = request.transactionIds().stream().distinct().toList();
        List<Transaction> chosen = transactionRepository.findAllById(userId, ids);
        if (chosen.size() != ids.size()) {
            throw new AccessDeniedException("Access denied to transaction");
        }

        Long merchantId = resolveMerchant(userId, request.rule());
        Long ruleId = createRule(userId, request.rule().toCreateRequest(merchantId));

        List<Transaction> toChange = chosen.stream()
                .filter(transaction -> transaction.getMerchant() == null
                        || !merchantId.equals(transaction.getMerchant().getId()))
                .toList();

        if (!toChange.isEmpty()) {
            transactionRepository.overwriteMerchant(userId, merchantId, toChange.stream().map(Transaction::getId).toList());
        }

        List<AppliedMerchantChangeDto> changes = toChange.stream()
                .map(transaction -> new AppliedMerchantChangeDto(transaction.getId(),
                        transaction.getMerchant() == null ? null : transaction.getMerchant().getId()))
                .toList();
        return new MerchantRuleApplyResultDto(ruleId, merchantId, changes);
    }
}
