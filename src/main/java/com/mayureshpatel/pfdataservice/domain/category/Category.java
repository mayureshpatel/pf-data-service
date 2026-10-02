package com.mayureshpatel.pfdataservice.domain.category;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A user-defined label for classifying transactions (e.g. "Groceries", "Rent"). Optionally
 * hierarchical: a category with {@code parentId} set is a subcategory of another {@code Category}
 * -- one level of nesting is the only structure this supports (a subcategory's own {@code parent}
 * is never itself a subcategory in practice, though nothing in this class enforces that).
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category {

    @EqualsAndHashCode.Include
    private Long id;
    private Long userId;
    private String name;
    private String type;
    private Long parentId;
    private String color;
    private String icon;
    private Category parent;

    @ToString.Exclude
    private TableAudit audit;

    /**
     * Returns true if this category is a subcategory.
     *
     * @return true if this category is a subcategory, false otherwise.
     */
    public boolean isSubCategory() {
        return parentId != null && parentId != 0;
    }
}
