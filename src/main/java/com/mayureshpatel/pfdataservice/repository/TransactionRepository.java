package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.dto.CategoryTotal;
import com.mayureshpatel.pfdataservice.dto.DailyBalance;
import com.mayureshpatel.pfdataservice.dto.MonthlySpending;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findByAccount_User_IdOrderByDateDesc(Long userId, Pageable pageable);
    
    Page<Transaction> findByAccount_User_IdAndType(Long userId, TransactionType type, Pageable pageable);

    @Query("""
            SELECT SUM(t.amount)
            FROM Transaction t
            JOIN t.account a
            JOIN a.user u
            WHERE u.id = :userId
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
            SELECT new com.mayureshpatel.pfdataservice.dto.CategoryTotal(COALESCE(c.name, 'Uncategorized'), SUM(t.amount))
            FROM Transaction t
                LEFT JOIN t.category c
            JOIN t.account a
            JOIN a.user u
            WHERE u.id = :userId
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
            SELECT new com.mayureshpatel.pfdataservice.dto.VendorTotal(COALESCE(t.vendorName, t.description), SUM(t.amount))
            FROM Transaction t
            JOIN t.account a
            JOIN a.user u
            WHERE u.id = :userId
                AND t.date BETWEEN :startDate AND :endDate
                AND t.type = 'EXPENSE'
            GROUP BY COALESCE(t.vendorName, t.description)
            ORDER BY SUM(t.amount) DESC
            """)
    List<com.mayureshpatel.pfdataservice.dto.VendorTotal> findVendorTotals(
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

    @Query("SELECT SUM(CASE WHEN t.type IN ('INCOME', 'TRANSFER_IN') THEN t.amount ELSE -t.amount END) FROM Transaction t WHERE t.account.id = :accountId AND t.date > :date")
    BigDecimal getNetFlowAfterDate(@Param("accountId") Long accountId, @Param("date") LocalDate date);

    @Query("""
            SELECT new com.mayureshpatel.pfdataservice.dto.DailyBalance(
                t.date,
                SUM(CASE WHEN t.type IN ('INCOME', 'TRANSFER_IN') THEN t.amount ELSE -t.amount END)
            )
            FROM Transaction t
            WHERE t.account.user.id = :userId
              AND t.date >= :startDate
            GROUP BY t.date
            ORDER BY t.date DESC
            """)
    List<DailyBalance> getDailyNetFlows(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate
    );

    long countByIdInAndAccount_User_Id(List<Long> ids, Long userId);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account a JOIN FETCH a.user u WHERE t.id IN :ids")
    List<Transaction> findAllByIdWithAccountAndUser(@Param("ids") List<Long> ids);

    Long countByAccountId(Long accountId);

    long countByCategoryId(Long categoryId);

    @Query("""
            SELECT t FROM Transaction t
            JOIN t.account a
            JOIN a.user u
            WHERE u.id = :userId
              AND t.type NOT IN ('TRANSFER', 'TRANSFER_IN', 'TRANSFER_OUT')
              AND t.date >= :startDate
            ORDER BY t.date DESC
            """)
    List<Transaction> findRecentNonTransferTransactions(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate
    );

    @Query("""
            SELECT YEAR(t.date) as year, MONTH(t.date) as month, t.type as type, SUM(t.amount) as total
            FROM Transaction t
            JOIN t.account a
            JOIN a.user u
            WHERE u.id = :userId
              AND t.type NOT IN ('TRANSFER', 'TRANSFER_IN', 'TRANSFER_OUT')
              AND t.date >= :startDate
            GROUP BY YEAR(t.date), MONTH(t.date), t.type
            ORDER BY YEAR(t.date), MONTH(t.date)
            """)
    List<Object[]> findMonthlySums(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate
    );

    List<Transaction> findByAccount_User_Id(Long userId);

    @Query("""
            SELECT t FROM Transaction t
            JOIN t.account a
            JOIN a.user u
            WHERE u.id = :userId
              AND t.type = 'EXPENSE'
              AND t.date >= :startDate
            ORDER BY t.date ASC
            """)
    List<Transaction> findExpensesSince(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate
    );
}