package com.mayureshpatel.pfdataservice.repository;

import java.time.OffsetDateTime;

/**
 * Support for audit fields
 */
public interface AuditSupport {

    /**
     * Get current timestamp for audit fields.
     *
     * @return current timestamp
     */
    default OffsetDateTime getCurrentTimestamp() {
        return OffsetDateTime.now();
    }

    /**
     * Build INSERT audit columns fragment.
     *
     * @param includeCreatedBy whether to include created_by column
     * @return INSERT audit columns fragment
     */
    default String insertAuditColumns(boolean includeCreatedBy) {
        return includeCreatedBy
                ? ", created_at, created_by"
                : ", created_at";
    }

    /**
     * Build UPDATE  audit columns fragment.
     *
     * @param includeUpdatedBy whether to include updated_by column
     * @return UPDATE audit columns fragment
     */
    default String updateAuditColumns(boolean includeUpdatedBy) {
        return includeUpdatedBy
                ? ", updated_at = CURRENT_TIMESTAMP, updated_by = ?"
                : ", updated_at = CURRENT_TIMESTAMP";
    }
}
