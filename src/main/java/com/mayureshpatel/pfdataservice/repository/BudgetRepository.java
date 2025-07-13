package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.domain.Budget;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends CrudRepository<Budget, UUID> {
    @Query("""
        SELECT * FROM budget
        WHERE user_id = :userId
            AND month_year = :monthStart
    """)
    Optional<Budget> findByUserAndMonth(UUID userId, LocalDate monthStart);
}
