package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.CategoryTotal;
import com.mayureshpatel.pfdataservice.dto.DailyBalance;
import com.mayureshpatel.pfdataservice.dto.DashboardData;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

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

    public List<DailyBalance> getNetWorthHistory(Long userId) {
        // Default to last 90 days if not specified, or just all time? 
        // Let's do 90 days for now as a sensible default for "History" graph
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90);

        BigDecimal currentNetWorth = accountRepository.sumCurrentBalanceByUserId(userId);
        if (currentNetWorth == null) currentNetWorth = BigDecimal.ZERO;

        List<DailyBalance> flows = transactionRepository.getDailyNetFlows(userId, startDate);
        Map<LocalDate, BigDecimal> flowMap = flows.stream()
                .collect(Collectors.toMap(DailyBalance::date, DailyBalance::balance));

        List<DailyBalance> history = new ArrayList<>();
        BigDecimal runningBalance = currentNetWorth;

        // Iterate backwards from today (inclusive) to startDate
        for (LocalDate date = endDate; !date.isBefore(startDate); date = date.minusDays(1)) {
            history.add(new DailyBalance(date, runningBalance));
            
            // Prepare balance for the previous day
            // Previous Balance = Current Balance - (Net Flow of Current Day)
            BigDecimal flow = flowMap.getOrDefault(date, BigDecimal.ZERO);
            runningBalance = runningBalance.subtract(flow);
        }

        Collections.reverse(history);
        return history;
    }
}
