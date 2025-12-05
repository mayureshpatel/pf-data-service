package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.dto.CategoryTotal;
import com.mayureshpatel.pfdataservice.dto.MonthlySpending;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByAccount_User_IdOrderByDateDesc(Long userId, Pageable pageable);

    @Query("""
            SELECT SUM(t.amount)
            FROM Transaction t
            WHERE t.account.user.id = :userId
                AND t.date BETWEEN :startDate AND :endDate
                AND t.type = :type
            """)
    BigDecimal getSumByDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("type") TransactionType type
    );

    @Query("""
            SELECT new com.mayureshpatel.pfdataservice.dto.CategoryTotal(c.name, SUM(t.amount))
            FROM Transaction t
                JOIN t.category c
            WHERE t.account.user.id = :userId
                AND t.date BETWEEN :startDate AND :endDate
                AND t.type = 'EXPENSE'
            GROUP BY c.name
            ORDER BY SUM(t.amount) DESC
            """)
    List<CategoryTotal> findCategoryTotals(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select sum(t.amount)
            from Transaction t
            where t.account.user.id = :userId
                and t.category is null
                and t.type = 'EXPENSE'
            """)
    BigDecimal getUncategorizedExpenseTotals(@Param("userId") Long userId);

    @Query("""
                SELECT new com.mayureshpatel.pfdataservice.dto.MonthlySpending(YEAR(t.date), MONTH(t.date), SUM(t.amount))
                FROM Transaction t
                WHERE t.account.user.id = :userId
                  AND t.date >= :startDate
                  AND t.type = 'EXPENSE'
                GROUP BY YEAR(t.date), MONTH(t.date)
                ORDER BY YEAR(t.date), MONTH(t.date)
            """)
    List<MonthlySpending> getMonthlySpending(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate
    );

    boolean existsByAccountIdAndDateAndAmountAndDescriptionAndType(
            Long accountId,
            LocalDate date,
            BigDecimal amount,
            String description,
            TransactionType type
    );

    @Query("""
            select t
            from Transaction t
                join t.tags tag
            where t.account.user.id = :userId
                and tag.name IN :tagNames
            group by t.id
            having count(distinct tag.id) = :tagCount
            """)
    List<Transaction> findByTagsAndLogic(
            @Param("userId") Long userId,
            @Param("tagNames") List<String> tagNames,
            @Param("tagCount") Long tagCount
    );
}
