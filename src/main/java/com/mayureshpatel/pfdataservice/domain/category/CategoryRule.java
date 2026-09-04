package com.mayureshpatel.pfdataservice.domain.category;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

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

    /**
     * Optional amount-range bounds (PF-314) -- when either is set, a transaction's absolute amount
     * must also fall within [minAmount, maxAmount] (open on whichever bound is null) for this rule
     * to match, in addition to the keyword match. Both null means the rule matches on keyword alone,
     * same as before this field existed.
     */
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    @ToString.Exclude
    private TableAudit audit;
}
