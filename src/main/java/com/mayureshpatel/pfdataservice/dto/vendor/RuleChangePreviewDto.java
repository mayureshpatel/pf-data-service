package com.mayureshpatel.pfdataservice.dto.vendor;

public record RuleChangePreviewDto(
        String description,
        String oldValue,
        String newValue
) {}
