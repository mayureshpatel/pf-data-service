package com.mayureshpatel.pfdataservice.domain.budget;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    private Long id;
    private User user;
    private Category category;
    private BigDecimal amount;

    private Integer month;
    private Integer year;

    private TableAudit audit;

    public BudgetDto toDto() {
        return new BudgetDto(
                id,
                user.getId(),
                category.toDto(),
                amount,
                month,
                year
        );
    }
}
