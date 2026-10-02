package com.mayureshpatel.pfdataservice.domain.budget;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * A user's budgeted spending {@code amount} for one {@code category} in one {@code month}/
 * {@code year} -- one row per category per calendar month, not a recurring/rolling allocation.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Budget {

    @EqualsAndHashCode.Include
    private Long id;
    private Long userId;
    private Category category;
    private BigDecimal amount;

    private Integer month;
    private Integer year;

    @ToString.Exclude
    private TableAudit audit;
}
