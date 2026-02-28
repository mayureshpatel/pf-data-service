package com.mayureshpatel.pfdataservice.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Audit fields for entities that only track creation and update timestamps.
 * Maps to tables with {@code created_at} and {@code updated_at} columns.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TimestampAudit {
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
