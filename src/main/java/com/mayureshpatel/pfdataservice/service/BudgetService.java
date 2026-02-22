package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * Gets budgets for a user given a specific month and year.
     *
     * @param userId the user id
     * @param month  the month
     * @param year   the year
     * @return the list of {@link BudgetDto}
     */
    public List<BudgetDto> getBudgets(Long userId, Integer month, Integer year) {
        return budgetRepository.findByUserIdAndMonthAndYearAndDeletedAtIsNull(userId, month, year)
                .stream()
                .map(BudgetDto::mapToDto)
                .toList();
    }

    /**
     * Gets all budgets for a user.
     *
     * @param userId the user id
     * @return the list of {@link BudgetDto}
     */
    public List<BudgetDto> getAllBudgets(Long userId) {
        return budgetRepository.findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(userId)
                .stream()
                .map(BudgetDto::mapToDto)
                .toList();
    }

    /**
     * Gets the budget status for a user for a given month and year.
     *
     * @param userId the user id
     * @param month  the month
     * @param year   the year
     * @return the list of {@link BudgetStatusDto}
     */
    public List<BudgetStatusDto> getBudgetStatus(Long userId, Integer month, Integer year) {
        return this.budgetRepository.findBudgetStatusByUserIdAndMonthAndYear(userId, month, year);
    }

    @Transactional
    public BudgetDto save(Long userId, BudgetDto dto) {
        // get the user; throw exception if not found
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // get the category; throw exception if not found
        Category category = categoryRepository.findById(dto.category().id())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // ensure user has access to the category
        if (!category.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied to category");
        }

        // check if budget already exists for the user, category, month and year
        Optional<Budget> existing = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(
                userId, dto.category().id(), dto.month(), dto.year());

        // create or update budget
        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setAmount(dto.amount());
        } else {
            budget = Budget.builder()
                    .user(user)
                    .category(category)
                    .amount(dto.amount())
                    .month(dto.month())
                    .year(dto.year())
                    .build();
        }

        return BudgetDto.mapToDto(budgetRepository.save(budget));
    }

    @Transactional
    public void delete(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        // ensure user has access to the budget
        if (!budget.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        budget.getAudit().setDeletedAt(OffsetDateTime.now());
        budgetRepository.save(budget);
    }
}
