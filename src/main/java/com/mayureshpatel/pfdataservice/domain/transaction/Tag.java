package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tag {

    @EqualsAndHashCode.Include
    private Long id;
    @ToString.Exclude
    private Long userId;
    private String name;
    private String color;

    @ToString.Exclude
    private TableAudit audit;
}
