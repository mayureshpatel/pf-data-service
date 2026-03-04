package com.mayureshpatel.pfdataservice.domain.category;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

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
