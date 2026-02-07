package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.model.AccountTypeLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountTypeLookupRepository extends JpaRepository<AccountTypeLookup, String> {

    List<AccountTypeLookup> findByIsActiveTrueOrderBySortOrder();
}
