package com.mayureshpatel.pfdataservice.application.mapper;

import com.mayureshpatel.pfdataservice.domain.Budget;
import com.mayureshpatel.pfdataservice.domain.BudgetItem;
import com.mayureshpatel.pfdataservice.web.dto.request.CreateBudgetRequest;
import com.mayureshpatel.pfdataservice.web.dto.response.BudgetResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;
import java.time.YearMonth;

@Mapper(componentModel = "spring")
public interface BudgetMapper {

    /* ---------- Request ➜ Domain ---------- */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "monthYear", expression = "java(request.month().atDay(1))")
    @Mapping(target = "items", expression = "java(toItems(request.items()))")
    Budget toDomain(CreateBudgetRequest request, UUID userId);

    /* ---------- Domain ➜ Response ---------- */

    @Mapping(target = "month", expression = "java(YearMonth.from(domain.getMonth()))")
    @Mapping(target = "items", expression = "java(toItemResponses(domain.getItems()))")
    BudgetResponse toResponse(Budget domain);

    /* ---------- Helpers ---------- */

    default List<BudgetItem> toItems(List<CreateBudgetRequest.ItemInput> inputs) {
        return inputs.stream()
                .map(i -> BudgetItem.builder()
                        .categoryId(i.getCategoryId())
                        .limitAmount(i.getLimitAmount())
                        .build())
                .toList();
    }

    default List<BudgetResponse.Item> toItemResponses(List<BudgetItem> items) {
        return items.stream()
                .map(it -> new BudgetResponse.Item(it.getCategoryId(),
                        it.getLimitAmount()))
                .toList();
    }
}

