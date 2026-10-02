package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A user-defined label attachable to transactions (many-to-many), orthogonal to
 * {@link com.mayureshpatel.pfdataservice.domain.category.Category} -- a transaction has exactly
 * one category but any number of tags, for cross-cutting groupings a single category hierarchy
 * can't express (e.g. "Tax Deductible", "Vacation 2026").
 */
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
