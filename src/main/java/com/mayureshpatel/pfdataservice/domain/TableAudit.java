package com.mayureshpatel.pfdataservice.domain;

import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * The standard created/updated/deleted audit trail embedded in every domain object backed by a
 * table that supports it -- who did it and when, for each of the three lifecycle events. Soft
 * deletion is what makes a dedicated {@code deletedBy}/{@code deletedAt} pair meaningful here: a
 * row marked deleted this way still exists, unlike a hard-deleted row, so knowing who deleted it
 * is recoverable information rather than something that vanished with the row itself.
 */
@Getter
@Builder(toBuilder = true)
public class TableAudit {
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private User createdBy;
    private User updatedBy;

    private User deletedBy;
    private OffsetDateTime deletedAt;

    /**
     * Builds the audit stamp for a brand-new row: created and updated both set to now, by the
     * same user.
     *
     * @param user the user performing the insert
     * @return a fresh audit stamp
     */
    public static TableAudit insertAudit(User user) {
        return TableAudit.builder()
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .createdBy(user)
                .updatedBy(user)
                .build();
    }

    /**
     * Builds the audit stamp for an update -- only {@code updatedAt}/{@code updatedBy} are set,
     * since {@code createdAt}/{@code createdBy} are immutable once a row exists.
     *
     * @param user the user performing the update
     * @return an audit stamp reflecting just the update
     */
    public static TableAudit updateAudit(User user) {
        return TableAudit.builder()
                .updatedAt(OffsetDateTime.now())
                .updatedBy(user)
                .build();
    }

    /**
     * Builds the audit stamp for a soft delete.
     *
     * @param user the user performing the delete
     * @return an audit stamp reflecting just the deletion
     */
    public static TableAudit deleteAudit(User user) {
        return TableAudit.builder()
                .deletedBy(user)
                .deletedAt(OffsetDateTime.now())
                .build();
    }

}
