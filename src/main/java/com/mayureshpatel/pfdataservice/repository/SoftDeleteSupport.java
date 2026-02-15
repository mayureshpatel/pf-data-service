package com.mayureshpatel.pfdataservice.repository;

/**
 * Interface for repositories that support soft deletes.
 * Provides default WHERE clause filtering for deleted_at IS NULL.
 */
public interface SoftDeleteSupport {

    /**
     * Standard WHERE clause to exclude soft-deleted records.
     *
     * @return the WHERE clause
     */
    default String getSoftDeleteFilter() {
        return "deleted_at IS NULL";
    }

    /**
     * Standard WHERE clause with AND prefix for combining with other conditions.
     *
     * @return the WHERE clause with AND prefix
     */
    default String andSoftDeleteFilter() {
        return " AND " + getSoftDeleteFilter();
    }

    /**
     * Soft delete UPDATE statement fragment.
     *
     * @param tableName the table name
     * @return the UPDATE statement with soft delete logic
     */
    default String softDeleteUpdate(String tableName) {
        return "UPDATE " + tableName + " SET deleted_at = CURRENT_TIMESTAMP";
    }
}
