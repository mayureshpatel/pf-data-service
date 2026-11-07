package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.AppliedCategoryChangeDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.CategoryRuleDtoMapper;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CRUD for keyword-based auto-categorization rules, plus {@link #previewApply} and
 * {@link #applyRules} for running the current rule set against a user's uncategorized
 * transactions. Delegates the actual matching logic to {@link TransactionCategorizer}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryRuleService {

    private final CategoryRuleRepository categoryRuleRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionCategorizer categorizer;
    private final CategoryRepository categoryRepository;

    /**
     * Get all category rules for a user
     *
     * @param userId the user id
     * @return the list of {@link CategoryRuleDto}
     */
    public List<CategoryRuleDto> getRules(Long userId) {
        return categoryRuleRepository.findByUserId(userId).stream()
                .map(CategoryRuleDtoMapper::toDto)
                .toList();
    }

    /**
     * Create a new category rule for a user
     *
     * @param userId  the user id
     * @param request the category rule create request
     * @return the newly created rule's generated id
     */
    @Transactional
    public Long createRule(Long userId, CategoryRuleCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied to category");
        }

        CategoryRule rule = CategoryRule.builder()
                .user(user)
                .keywords(request.getKeywords())
                .matchType(request.getMatchType() != null ? request.getMatchType() : MatchType.OR)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .category(category)
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .audit(TableAudit.insertAudit(user))
                .build();

        return categoryRuleRepository.insertAndReturnId(rule);
    }

    /**
     * Update an existing category rule for a user
     *
     * @param userId  the user id
     * @param request the updated category rule request
     * @return the updated {@link CategoryRuleDto}
     */
    @Transactional
    public int updateRule(Long userId, CategoryRuleUpdateRequest request) {
        CategoryRule rule = categoryRuleRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));

        if (!rule.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this rule");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied to category");
        }

        CategoryRule updatedRule = rule.toBuilder()
                .keywords(request.getKeywords())
                .matchType(request.getMatchType() != null ? request.getMatchType() : MatchType.OR)
                .category(category)
                .priority(request.getPriority())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .audit(TableAudit.updateAudit(rule.getUser()))
                .build();

        return categoryRuleRepository.update(updatedRule);
    }

    /**
     * Delete an existing category rule for a user
     *
     * @param userId the user id
     * @param ruleId the rule id to delete
     */
    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        CategoryRule rule = categoryRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));

        if (!rule.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this rule");
        }

        categoryRuleRepository.deleteById(ruleId, userId);
    }

    /**
     * Previews which currently-uncategorized transactions would be recategorized if the user's
     * rule set were applied, without changing anything.
     *
     * @param userId the user id
     * @return the transactions that would change, and what category they'd get
     */
    public List<RuleChangePreviewDto> previewApply(Long userId) {
        List<CategoryRule> rules = categoryRuleRepository.findByUserId(userId);
        List<Category> categories = categoryRepository.findByUserId(userId);
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        category -> category, (categoryA, categoryB) -> categoryA)
                );

        List<RuleChangePreviewDto> previews = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.getCategory() != null) {
                continue;
            }

            Long guessedCategory = this.categorizer.guessCategory(transaction, rules, categories);
            if (guessedCategory == null || guessedCategory <= 0) {
                continue;
            }

            Category matchedCategory = categoryMap.get(guessedCategory);
            if (matchedCategory == null) {
                continue;
            }

            previews.add(new RuleChangePreviewDto(
                    transaction.getDescription(),
                    "Uncategorized",
                    matchedCategory.getName()
            ));
        }

        return previews;
    }

    /**
     * Applies the user's rule set to their currently-uncategorized transactions, assigning a
     * category to each one a rule matches.
     *
     * @param userId the user id
     * @return the number of transactions recategorized
     */
    @Transactional
    public int applyRules(Long userId) {
        List<CategoryRule> rules = this.categoryRuleRepository.findByUserId(userId);
        List<Category> categories = this.categoryRepository.findByUserId(userId);
        List<Transaction> transactions = this.transactionRepository.findByUserId(userId);

        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        category -> category, (categoryA, categoryB) -> categoryA));

        List<Transaction> toUpdate = new ArrayList<>();

        for (Transaction transaction : transactions) {
            if (transaction.getCategory() != null) {
                continue;
            }

            Long guessedCategory = this.categorizer.guessCategory(transaction, rules, categories);
            if (guessedCategory == null || guessedCategory <= 0) {
                continue;
            }

            Category matchedCategory = categoryMap.get(guessedCategory);
            if (matchedCategory == null) {
                continue;
            }

            toUpdate.add(transaction.toBuilder()
                    .category(matchedCategory)
                    .build());
        }

        if (!toUpdate.isEmpty()) {
            this.transactionRepository.updateAll(userId, toUpdate);
        }

        return toUpdate.size();
    }

    /**
     * PF-920: creates a category rule and applies it, in one all-or-nothing step, to the
     * transactions the user chose, overwriting any category they have. Only the chosen
     * transactions change (never anything else), transfers are skipped and reported because rules
     * never apply to them, and a transaction that already has the rule's category is left alone.
     * Every transaction that did change is returned with its previous category so the change can be
     * undone. Settings "Apply rules" and import are untouched: they still only fill blanks.
     *
     * <p>Everything is checked before the first write (every chosen id must be the user's, the
     * category must be theirs), and the whole method is one transaction, so a failure part-way,
     * after the rule was saved, rolls the rule back too.
     *
     * @param userId  the user id
     * @param request the rule to create and the chosen transaction ids
     * @return the new rule id, the changed transactions with their previous categories, and the skipped ids
     * @throws AccessDeniedException if a chosen transaction or the category is not the user's
     */
    @Transactional
    public CategoryRuleApplyResultDto createAndApply(Long userId, CategoryRuleApplyRequest request) {
        List<Long> ids = request.transactionIds().stream().distinct().toList();
        List<Transaction> chosen = transactionRepository.findAllById(userId, ids);
        if (chosen.size() != ids.size()) {
            throw new AccessDeniedException("Access denied to transaction");
        }

        Long categoryId = request.rule().getCategoryId();
        Long ruleId = createRule(userId, request.rule());

        List<Long> skipped = chosen.stream()
                .filter(transaction -> isTransferType(transaction.getType()))
                .map(Transaction::getId)
                .toList();
        List<Transaction> toChange = chosen.stream()
                .filter(transaction -> !isTransferType(transaction.getType()))
                .filter(transaction -> transaction.getCategory() == null
                        || !categoryId.equals(transaction.getCategory().getId()))
                .toList();

        if (!toChange.isEmpty()) {
            transactionRepository.overwriteCategory(userId, categoryId, toChange.stream().map(Transaction::getId).toList());
        }

        List<AppliedCategoryChangeDto> changes = toChange.stream()
                .map(transaction -> new AppliedCategoryChangeDto(transaction.getId(),
                        transaction.getCategory() == null ? null : transaction.getCategory().getId()))
                .toList();
        return new CategoryRuleApplyResultDto(ruleId, categoryId, changes, skipped);
    }

    /** True for the transfer types, which rules never categorize. */
    private static boolean isTransferType(TransactionType type) {
        return type == TransactionType.TRANSFER
                || type == TransactionType.TRANSFER_IN
                || type == TransactionType.TRANSFER_OUT;
    }
}
