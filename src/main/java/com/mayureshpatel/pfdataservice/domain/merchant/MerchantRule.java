package com.mayureshpatel.pfdataservice.domain.merchant;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.rule.MatchableRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * A user-defined merchant rule (PF-904): when a transaction's description matches this rule's
 * {@link #keywords} (combined per {@link #matchType}) and its amount falls within the optional
 * {@link #minAmount}/{@link #maxAmount} bounds, the rule points at its {@link #merchant}.
 * {@link #priority} breaks ties when more than one rule matches the same transaction. It mirrors
 * {@link com.mayureshpatel.pfdataservice.domain.category.CategoryRule} and shares the
 * {@link MatchableRule} contract so both rule types match and rank identically.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MerchantRule implements MatchableRule {

    @EqualsAndHashCode.Include
    private Long id;
    private User user;

    /** The rule's keyword set; always at least one keyword in practice (enforced at the API boundary, not the DB). */
    private List<String> keywords;

    /** How {@link #keywords} combine: {@code AND} requires every keyword, {@code OR} at least one. */
    private MatchType matchType;

    private Integer priority;
    private Merchant merchant;

    /** Optional inclusive bounds on a transaction's absolute amount; both null means keyword-only matching. */
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    @ToString.Exclude
    private TableAudit audit;
}
