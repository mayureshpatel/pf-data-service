package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleUpdateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
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
     * @return the created {@link CategoryRuleDto}
     */
    @Transactional
    public int createRule(Long userId, CategoryRuleCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        CategoryRule rule = CategoryRule.builder()
                .user(user)
                .keyword(request.getKeyword())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .category(category)
                .audit(TableAudit.insertAudit(user))
                .build();

        return categoryRuleRepository.insert(rule);
    }

    /**
     * Update an existing category rule for a user
     *
     * @param userId  the user id
     * @param ruleId  the rule id to update
     * @param request the updated category rule request
     * @return the updated {@link CategoryRuleDto}
     */
    @Transactional
    public int updateRule(Long userId, Long ruleId, CategoryRuleUpdateRequest request) {
        CategoryRule rule = categoryRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));

        if (!rule.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this rule");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        CategoryRule updatedRule = rule.toBuilder()
                .category(category)
                .priority(request.getPriority())
                .audit(TableAudit.updateAudit(rule.getUser()))
                .build();

        return categoryRuleRepository.save(updatedRule);
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

        categoryRuleRepository.deleteById(ruleId);
    }

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
            if (guessedCategory == null) {
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

    @Transactional
    public int applyRules(Long userId) {
        List<CategoryRule> rules = this.categoryRuleRepository.findByUserId(userId);
        List<Category> categories = this.categoryRepository.findByUserId(userId);
        List<TransactionUpdateRequest> transactions = this.transactionRepository.findByUserId(userId)
                .stream()
                .map(TransactionUpdateRequest::fromDomain)
                .toList();

        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        category -> category, (categoryA, categoryB) -> categoryA));

        List<TransactionUpdateRequest> toUpdate = new ArrayList<>();

        for (TransactionUpdateRequest t : transactions) {
            if (t.getCategoryId() != null) {
                continue;
            }

            Long guessedCategory = this.categorizer.guessCategory(userId, t, rules, categories);
            if (null == guessedCategory) {
                continue;
            }

            Category matchedCategory = categoryMap.get(guessedCategory);
            if (matchedCategory == null) {
                continue;
            }

            t.toBuilder()
                    .categoryId(matchedCategory.getId())
                    .build();
            toUpdate.add(t);
        }

        if (!toUpdate.isEmpty()) {
            this.transactionRepository.updateAll(toUpdate);
        }

        return toUpdate.size();
    }
}
