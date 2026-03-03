package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tag {

    private Long id;
    @ToString.Exclude
    private User user;
    private String name;
    private String color;

    @ToString.Exclude
    private TimestampAudit audit;
}
