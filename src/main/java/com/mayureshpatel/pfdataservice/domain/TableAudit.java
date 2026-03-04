package com.mayureshpatel.pfdataservice.domain;

import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
public class TableAudit {
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private User createdBy;
    private User updatedBy;

    private User deletedBy;
    private OffsetDateTime deletedAt;

    public static TableAudit insertAudit(User user) {
        return TableAudit.builder()
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .createdBy(user)
                .updatedBy(user)
                .build();
    }

    public static TableAudit updateAudit(User user) {
        return TableAudit.builder()
                .updatedAt(OffsetDateTime.now())
                .updatedBy(user)
                .build();
    }

    public static TableAudit deleteAudit(User user) {
        return TableAudit.builder()
                .deletedBy(user)
                .deletedAt(OffsetDateTime.now())
                .build();
    }

}
