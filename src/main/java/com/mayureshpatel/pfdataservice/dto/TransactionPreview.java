package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TransactionPreview {
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private TransactionType type;
    private String suggestedCategory;
}
