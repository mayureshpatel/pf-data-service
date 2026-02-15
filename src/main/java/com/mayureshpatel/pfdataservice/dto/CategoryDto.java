package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.repository.category.model.CategoryType;
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
