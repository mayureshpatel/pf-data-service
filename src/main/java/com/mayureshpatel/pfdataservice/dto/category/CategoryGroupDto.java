package com.mayureshpatel.pfdataservice.dto.category;

import java.util.List;

public record CategoryGroupDto(
    String groupLabel,      // Parent category name
    Long groupId,           // Parent category ID
    List<CategoryDto> items // Child categories
) {}
