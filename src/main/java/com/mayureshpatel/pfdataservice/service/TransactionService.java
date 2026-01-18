package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.CategoryTotal;
import com.mayureshpatel.pfdataservice.dto.DashboardData;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public DashboardData getDashboardData(Long userId, int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        BigDecimal income = this.transactionRepository.getSumByDateRange(userId, startDate, endDate, TransactionType.INCOME);
        BigDecimal expense = this.transactionRepository.getSumByDateRange(userId, startDate, endDate, TransactionType.EXPENSE);

        income = (income == null) ? BigDecimal.ZERO : income;
        expense = (expense == null) ? BigDecimal.ZERO : expense;

        List<CategoryTotal> breakdown = this.transactionRepository.findCategoryTotals(userId, startDate, endDate);

        return DashboardData.builder()
                .totalIncome(income)
                .totalExpense(expense)
                .netSavings(income.subtract(expense))
                .categoryBreakdown(breakdown)
                .build();
    }
}