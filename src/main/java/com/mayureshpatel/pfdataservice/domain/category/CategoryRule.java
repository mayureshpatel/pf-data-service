package com.mayureshpatel.pfdataservice.domain.category;

import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CategoryRule {

    @EqualsAndHashCode.Include
    private Long id;
    private User user;
    private String keyword;
    private Integer priority;
    private Category category;

    @ToString.Exclude
    private TimestampAudit audit;
}
