package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.model.VendorRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorRuleRepository extends JpaRepository<VendorRule, Long> {

    @Query("SELECT r FROM VendorRule r WHERE r.user.id = :userId OR r.user IS NULL ORDER BY r.priority DESC, LENGTH(r.keyword) DESC")
    List<VendorRule> findByUserOrGlobal(Long userId);
}
