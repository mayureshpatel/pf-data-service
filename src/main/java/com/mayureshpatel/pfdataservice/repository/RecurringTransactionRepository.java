package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.model.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserIdAndActiveTrueOrderByNextDateAsc(Long userId);

    List<RecurringTransaction> findByUserIdAndActiveTrueAndNextDateBeforeOrderByNextDateAsc(Long userId, LocalDate date);
}
