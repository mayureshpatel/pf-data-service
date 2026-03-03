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
}
