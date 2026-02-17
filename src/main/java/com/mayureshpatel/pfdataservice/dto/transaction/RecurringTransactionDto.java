package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record RecurringTransactionDto(
    Long id,
    
    Long accountId,
    String accountName,
    
    @NotBlank(message = "Merchant name is required")
    String merchantName,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", message = "Amount must be at least 0")
    BigDecimal amount,
    
    @NotNull(message = "Frequency is required")
    Frequency frequency,
    
    LocalDate lastDate,
    
    @NotNull(message = "Next date is required")
    LocalDate nextDate,
    
    boolean active
) {}
