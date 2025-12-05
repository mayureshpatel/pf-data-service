package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.model.Tag;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository {
    List<Tag> findByUserId(Long userId);

    Optional<Tag> findByUserIdAndName(Long userId, String name);

    Optional<Tag> findByUserIdAndNameIgnoreCase(Long userId, String name);

    List<Tag> findByUserIdAndNameIn(Long userId, List<String> names);

    
}
