package com.mayureshpatel.pfdataservice.domain.category;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRule {

    private Long id;
    private User user;
    private String keyword;
    private Integer priority;
    private Category category;

    private TableAudit audit;

    public CategoryRuleDto toDto() {
        return new CategoryRuleDto(
                id,
                user.getId(),
                keyword,
                priority,
                category.toDto()
        );
    }
}