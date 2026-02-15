package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import jakarta.validation.constraints.NotBlank;

public record CategoryDto(
    Long id,

    @NotBlank(message = "Name is required")
    String name,

    String color,
    String icon,
    CategoryType type,
    
    Long parentId,
    String parentName
) {}
