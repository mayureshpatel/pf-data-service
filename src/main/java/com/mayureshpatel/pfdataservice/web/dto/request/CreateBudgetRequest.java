package com.mayureshpatel.pfdataservice.web.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class CreateBudgetRequest {
    private YearMonth month;
    private List<@Valid ItemInput> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor(force = true)
    public static class ItemInput {
        private UUID categoryId;
        private long limitAmount;
    }
}
