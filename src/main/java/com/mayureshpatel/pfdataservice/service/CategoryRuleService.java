package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.jdbc.repository.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.model.Category;
import com.mayureshpatel.pfdataservice.model.CategoryRule;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.User;
import com.mayureshpatel.pfdataservice.repository.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.UserRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import jakarta.persistence.EntityNotFoundException;
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

    public List<CategoryRuleDto> getRules(Long userId) {
        return categoryRuleRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public CategoryRuleDto createRule(Long userId, CategoryRuleDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        CategoryRule rule = new CategoryRule();
        rule.setUser(user);
        rule.setKeyword(dto.keyword());
        rule.setCategoryName(dto.categoryName());
        rule.setPriority(dto.priority() != null ? dto.priority() : 0);

        return mapToDto(categoryRuleRepository.save(rule));
    }

    @Transactional
    public CategoryRuleDto updateRule(Long ruleId, CategoryRuleDto dto) {
        CategoryRule rule = categoryRuleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found"));
        rule.setKeyword(dto.keyword());
        rule.setCategoryName(dto.categoryName());
        rule.setPriority(dto.priority());
        rule.setUpdatedAt(OffsetDateTime.now());

        return mapToDto(categoryRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        CategoryRule rule = categoryRuleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found"));

        if (!rule.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this rule");
        }

        categoryRuleRepository.deleteById(ruleId);
    }

    public List<RuleChangePreviewDto> previewApply(Long userId) {
        List<CategoryRule> rules = categoryRuleRepository.findByUserId(userId);
        List<Category> categories = categoryRepository.findByUserId(userId);
        List<Transaction> transactions = transactionRepository.findByAccount_User_Id(userId);

        Map<String, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(), c -> c, (a, b) -> a));

        List<RuleChangePreviewDto> previews = new ArrayList<>();

        for (Transaction t : transactions) {
            if (t.getCategory() != null) continue;

            String guessed = categorizer.guessCategory(t, rules, categories);
            if ("Uncategorized".equals(guessed)) continue;

            Category matchedCategory = categoryMap.get(guessed.toLowerCase());
            if (matchedCategory == null) continue;

            previews.add(new RuleChangePreviewDto(
                    t.getDescription(),
                    "Uncategorized",
                    matchedCategory.getName()
            ));
        }

        return previews;
    }

    @Transactional
    public int applyRules(Long userId) {
        List<CategoryRule> rules = categoryRuleRepository.findByUserId(userId);
        List<Category> categories = categoryRepository.findByUserId(userId);
        List<Transaction> transactions = transactionRepository.findByAccount_User_Id(userId);

        Map<String, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(), c -> c, (a, b) -> a));

        List<Transaction> toUpdate = new ArrayList<>();

        for (Transaction t : transactions) {
            if (t.getCategory() != null) continue;

            String guessed = categorizer.guessCategory(t, rules, categories);
            if ("Uncategorized".equals(guessed)) continue;

            Category matchedCategory = categoryMap.get(guessed.toLowerCase());
            if (matchedCategory == null) continue;

            t.setCategory(matchedCategory);
            toUpdate.add(t);
        }

        if (!toUpdate.isEmpty()) {
            transactionRepository.saveAll(toUpdate);
        }

        return toUpdate.size();
    }

    private CategoryRuleDto mapToDto(CategoryRule rule) {
        return CategoryRuleDto.builder()
                .id(rule.getId())
                .keyword(rule.getKeyword())
                .categoryName(rule.getCategoryName())
                .priority(rule.getPriority())
                .build();
    }
}
