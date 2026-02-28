package com.mayureshpatel.pfdataservice.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Audit fields for entities that only track creation timestamp.
 * Maps to tables with only a {@code created_at} (or {@code imported_at}) column.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreatedAtAudit {
    private OffsetDateTime createdAt;
}
