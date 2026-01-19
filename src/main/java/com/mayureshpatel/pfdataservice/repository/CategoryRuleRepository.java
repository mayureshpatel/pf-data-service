package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.model.CategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {
    
    @Query("SELECT r FROM CategoryRule r ORDER BY r.priority DESC, LENGTH(r.keyword) DESC")
    List<CategoryRule> findAllOrdered();

    @Query("SELECT r FROM CategoryRule r WHERE r.user.id = :userId OR r.user IS NULL ORDER BY r.priority DESC, LENGTH(r.keyword) DESC")
    List<CategoryRule> findByUserOrGlobal(Long userId);
}