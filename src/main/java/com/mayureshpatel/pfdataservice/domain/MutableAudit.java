package com.mayureshpatel.pfdataservice.domain;

import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MutableAudit {
    private OffsetDateTime createdAt;
    private User createdBy;
    private OffsetDateTime updatedAt;
    private User updatedBy;

}
