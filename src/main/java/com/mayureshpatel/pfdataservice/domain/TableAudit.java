package com.mayureshpatel.pfdataservice.domain;

import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TableAudit {
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private User createdBy;
    private User updatedBy;

    private User deletedBy;
    private OffsetDateTime deletedAt;
}
