package com.mayureshpatel.pfdataservice.domain.category;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CategoryRule {

    @EqualsAndHashCode.Include
    private Long id;
    private User user;

    /**
     * A rule's keyword set (PF-315) -- always at least one keyword in practice (enforced at the
     * API boundary, not the DB). A single-keyword rule is the degenerate case: one element,
     * {@link #matchType} {@code OR}, matching the original single-keyword behavior exactly.
     */
    private List<String> keywords;

    /**
     * How {@link #keywords} combine when matching a description (PF-315): {@code AND} requires
     * every keyword present, {@code OR} requires at least one.
     */
    private MatchType matchType;

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
