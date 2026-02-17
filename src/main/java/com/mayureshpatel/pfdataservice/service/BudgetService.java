package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.BudgetDto;
import com.mayureshpatel.pfdataservice.dto.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.dto.CategoryTotal;
import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public List<BudgetDto> getBudgets(Long userId, Integer month, Integer year) {
        return budgetRepository.findByUserIdAndMonthAndYearAndDeletedAtIsNull(userId, month, year)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<BudgetDto> getAllBudgets(Long userId) {
        return budgetRepository.findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(userId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<BudgetStatusDto> getBudgetStatus(Long userId, Integer month, Integer year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 1. Get all budgets for the period
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYearAndDeletedAtIsNull(userId, month, year);
        
        // 2. Get actual spending for the period
        List<CategoryTotal> spending = transactionRepository.findCategoryTotals(userId, startDate, endDate);
        Map<String, BigDecimal> spendingMap = spending.stream()
                .collect(Collectors.toMap(CategoryTotal::categoryName, CategoryTotal::total));

        // 3. Map budgets to status DTOs
        List<BudgetStatusDto> status = budgets.stream().map(b -> {
            BigDecimal spent = spendingMap.getOrDefault(b.getCategory().getName(), BigDecimal.ZERO);
            BigDecimal remaining = b.getAmount().subtract(spent);
            double percent = calculatePercentage(spent, b.getAmount());
            
            return BudgetStatusDto.builder()
                    .categoryId(b.getCategory().getId())
                    .categoryName(b.getCategory().getName())
                    .budgetedAmount(b.getAmount())
                    .spentAmount(spent)
                    .remainingAmount(remaining)
                    .percentageUsed(percent)
                    .build();
        }).collect(Collectors.toList());

        // 4. Optionally add categories that have spending but NO budget
        // This helps user see where they are spending without a plan
        for (CategoryTotal s : spending) {
            boolean hasBudget = status.stream().anyMatch(st -> st.categoryName().equals(s.categoryName()));
            if (!hasBudget) {
                status.add(BudgetStatusDto.builder()
                        .categoryName(s.categoryName())
                        .budgetedAmount(BigDecimal.ZERO)
                        .spentAmount(s.total())
                        .remainingAmount(s.total().negate())
                        .percentageUsed(100.0) // Technically infinite but 100 is a good flag
                        .build());
            }
        }

        return status;
    }

    private double calculatePercentage(BigDecimal spent, BigDecimal budgeted) {
        if (budgeted.compareTo(BigDecimal.ZERO) == 0) return 0;
        return spent.divide(budgeted, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    @Transactional
    public BudgetDto setBudget(Long userId, BudgetDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied to category");
        }

        Optional<Budget> existing = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(
                userId, dto.categoryId(), dto.month(), dto.year());

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

        return mapToDto(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(Long userId, Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        budget.setDeletedAt(java.time.LocalDateTime.now());
        budgetRepository.save(budget);
    }

    private BudgetDto mapToDto(Budget b) {
        return BudgetDto.builder()
                .id(b.getId())
                .categoryId(b.getCategory().getId())
                .categoryName(b.getCategory().getName())
                .amount(b.getAmount())
                .month(b.getMonth())
                .year(b.getYear())
                .build();
    }
}
