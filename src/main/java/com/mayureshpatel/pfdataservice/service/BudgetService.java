package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetCreateRequest;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.BudgetDtoMapper;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                .map(BudgetDtoMapper::toDto)
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
                .map(BudgetDtoMapper::toDto)
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
    public int create(Long userId, BudgetCreateRequest request) {
        // get the user; throw exception if not found
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // get the category; throw exception if not found
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // ensure user has access to the category
        if (!category.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied to category");
        }

        // check if budget already exists for the user, category, month and year
        budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(
                userId, request.getCategoryId(), request.getMonth(), request.getYear());

        return budgetRepository.insert(request);
    }

    @Transactional
    public int update(Long userId, BudgetUpdateRequest request) {
        // check if the budget exists
        Budget existingBudget = budgetRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        // ensure user has access to the budget
        if (!existingBudget.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        return budgetRepository.update(request);
    }

    @Transactional
    public void delete(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        // ensure user has access to the budget
        if (!budget.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
        budgetRepository.deleteById(budgetId);
    }
}
