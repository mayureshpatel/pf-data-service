package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.application.mapper.BudgetMapper;
import com.mayureshpatel.pfdataservice.domain.Budget;
import com.mayureshpatel.pfdataservice.repository.BudgetRepository;
import com.mayureshpatel.pfdataservice.service.exception.BudgetNotFoundException;
import com.mayureshpatel.pfdataservice.service.exception.DuplicateBudgetException;
import com.mayureshpatel.pfdataservice.web.dto.request.CreateBudgetRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Transactional
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;

    public Budget create(CreateBudgetRequest request, UUID userId) {
        LocalDate monstStart = request.getMonth().atDay(1);

        budgetRepository.findByUserAndMonth(userId, monstStart)
                .ifPresent(budget -> {
                            throw new DuplicateBudgetException(userId, request.getMonth());
                        }
                );

        Budget domain = budgetMapper.toDomain(request, userId);
        try {
            return budgetRepository.save(domain);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateBudgetException(userId, request.getMonth(), e);
        }
    }

    public Budget fetch(UUID userId, YearMonth month) {
        return budgetRepository.findByUserAndMonth(userId, month.atDay(1))
                .orElseThrow(() -> new BudgetNotFoundException(userId, month));
    }
}
