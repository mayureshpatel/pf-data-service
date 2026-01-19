package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.model.AccountSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountSnapshotRepository extends JpaRepository<AccountSnapshot, Long> {
    
    Optional<AccountSnapshot> findByAccountIdAndSnapshotDate(Long accountId, LocalDate snapshotDate);
    
    List<AccountSnapshot> findByAccountIdOrderBySnapshotDateAsc(Long accountId);
}
