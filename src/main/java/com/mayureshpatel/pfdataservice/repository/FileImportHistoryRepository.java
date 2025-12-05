package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.model.FileImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileImportHistoryRepository extends JpaRepository<FileImportHistory, Long> {
    boolean existsByAccountIdAndFileHash(Long accountId, String fileHash);
}
