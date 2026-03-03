package com.mayureshpatel.pfdataservice.domain.budget;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Budget {

    @EqualsAndHashCode.Include
    private Long id;
    private User user;
    private Category category;
    private BigDecimal amount;

    private Integer month;
    private Integer year;

    @ToString.Exclude
    private SoftDeleteAudit audit;
}
