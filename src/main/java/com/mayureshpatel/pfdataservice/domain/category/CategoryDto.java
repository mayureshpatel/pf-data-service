package com.mayureshpatel.pfdataservice.domain.category;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDto {

    private Long id;
    private User user;
    private String name;
    private CategoryType type;
    private CategoryDto parent;

    private Iconography iconography;
    private TableAudit audit;

    public CategoryDto(com.mayureshpatel.pfdataservice.dto.category.CategoryDto category) {
        this.id = category.id();
        this.user = category.user();
        this.name = category.name();
        this.type = category.categoryType();

        if (category.parent() != null) {
            this.parent = new CategoryDto(category.parent());
        }

        this.iconography = category.iconography();
    }
}