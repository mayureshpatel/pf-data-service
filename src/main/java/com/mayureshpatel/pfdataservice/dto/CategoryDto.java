package com.mayureshpatel.pfdataservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryDto(
    Long id,

    @NotBlank(message = "Name is required")
    String name
) {}
