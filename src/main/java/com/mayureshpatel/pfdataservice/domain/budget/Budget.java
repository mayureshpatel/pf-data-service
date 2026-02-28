package com.mayureshpatel.pfdataservice.domain.budget;

import com.mayureshpatel.pfdataservice.domain.SoftDeleteAudit;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    private Long id;
    @ToString.Exclude
    private User user;
    @ToString.Exclude
    private Category category;
    private BigDecimal amount;

    private Integer month;
    private Integer year;

    @ToString.Exclude
    private SoftDeleteAudit audit;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Budget budget = (Budget) o;
        return id != null && id.equals(budget.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
