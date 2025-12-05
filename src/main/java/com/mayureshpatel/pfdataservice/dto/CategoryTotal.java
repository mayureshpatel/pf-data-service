package com.mayureshpatel.pfdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryTotal {

    private String categoryName;
    private BigDecimal total;
}
