package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
                .map(CategoryRuleDto::mapToDto)
                .toList();
    }

    /**
     * Create a new category rule for a user
     *
     * @param userId the user id
     * @param dto    the category rule dto
     * @return the created {@link CategoryRuleDto}
     */
    @Transactional
    public CategoryRuleDto createRule(Long userId, CategoryRuleDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = new Category();
        category.setId(dto.category().id());

        TableAudit audit = new TableAudit();
        audit.setCreatedAt(OffsetDateTime.now());
        audit.setUpdatedAt(OffsetDateTime.now());

        CategoryRule rule = new CategoryRule(
                null,
                dto.keyword(),
                category,
                dto.priority() != null ? dto.priority() : 0,
                user,
                audit
        );

        return CategoryRuleDto.mapToDto(categoryRuleRepository.save(rule));
    }

    /**
     * Update an existing category rule for a user
     *
     * @param ruleId the rule id to update
     * @param dto    the updated category rule dto
     * @return the updated {@link CategoryRuleDto}
     */
    @Transactional
    public CategoryRuleDto updateRule(Long ruleId, CategoryRuleDto dto) {
        CategoryRule rule = categoryRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));

        Category category = new Category();
        category.setId(dto.category().id());

        rule.setKeyword(dto.keyword());
        rule.setCategory(category);
        rule.setPriority(dto.priority());
        rule.getAudit().setUpdatedAt(OffsetDateTime.now());

        return CategoryRuleDto.mapToDto(categoryRuleRepository.save(rule));
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
        List<Transaction> transactions = this.transactionRepository.findByUserId(userId);

        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        category -> category, (categoryA, categoryB) -> categoryA));

        List<Transaction> toUpdate = new ArrayList<>();

        for (Transaction t : transactions) {
            if (t.getCategory() != null) {
                continue;
            }

            Long guessedCategory = this.categorizer.guessCategory(t, rules, categories);
            if (null == guessedCategory) {
                continue;
            }

            Category matchedCategory = categoryMap.get(guessedCategory);
            if (matchedCategory == null) {
                continue;
            }

            t.setCategory(matchedCategory);
            toUpdate.add(t);
        }

        if (!toUpdate.isEmpty()) {
            this.transactionRepository.saveAll(toUpdate);
        }

        return toUpdate.size();
    }
}
