package com.mayureshpatel.pfdataservice.domain.category;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.user.User;
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
    private User user;
    private String name;
    private CategoryType type;
    private Category parent;

    @ToString.Exclude
    private Iconography iconography;
    @ToString.Exclude
    private TimestampAudit audit;

    /**
     * Returns true if this category is a subcategory.
     *
     * @return true if this category is a subcategory, false otherwise.
     */
    public boolean isSubCategory() {
        return parent != null;
    }
}
