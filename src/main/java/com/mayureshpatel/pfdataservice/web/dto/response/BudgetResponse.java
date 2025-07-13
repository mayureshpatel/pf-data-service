package com.mayureshpatel.pfdataservice.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class BudgetResponse {
    private UUID id;
    private YearMonth month;
    List<Item> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor(force = true)
    public static class Item {
        private UUID categoryId;
        private long limitAmount;
    }
}
