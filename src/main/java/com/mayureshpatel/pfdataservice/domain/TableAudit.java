package com.mayureshpatel.pfdataservice.domain;

import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TableAudit {
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private User createdBy;
    private User updatedBy;

    private User deletedBy;
    private OffsetDateTime deletedAt;
}
