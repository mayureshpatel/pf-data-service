package com.mayureshpatel.pfdataservice.domain.budget;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Budget {

    @EqualsAndHashCode.Include
    private Long id;
    private Long userId;
    private Long categoryId;
    private BigDecimal amount;

    private Integer month;
    private Integer year;

    @ToString.Exclude
    private TableAudit audit;
}
