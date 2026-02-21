package com.mayureshpatel.pfdataservice.dto;

public record RuleChangePreviewDto(
        String description,
        String oldValue,
        String newValue
) {}
