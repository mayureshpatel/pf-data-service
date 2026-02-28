package com.mayureshpatel.pfdataservice.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Audit fields for entities that track timestamps and support soft deletes,
 * but do not track which user performed the action.
 * Maps to tables with {@code created_at}, {@code updated_at}, and {@code deleted_at} columns.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SoftDeleteAudit {
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
