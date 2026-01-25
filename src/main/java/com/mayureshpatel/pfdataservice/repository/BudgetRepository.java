package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    List<Budget> findByUserIdAndMonthAndYearAndDeletedAtIsNull(Long userId, Integer month, Integer year);
    
    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(Long userId, Long categoryId, Integer month, Integer year);
}
