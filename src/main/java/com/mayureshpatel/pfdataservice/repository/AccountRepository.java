package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserId(Long userId);

    @Query("SELECT SUM(a.currentBalance) FROM Account a WHERE a.user.id = :userId")
    BigDecimal sumCurrentBalanceByUserId(@Param("userId") Long userId);
}
