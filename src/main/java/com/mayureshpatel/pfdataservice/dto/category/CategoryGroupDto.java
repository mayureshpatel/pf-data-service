package com.mayureshpatel.pfdataservice.dto.category;

import java.util.List;

public record CategoryGroupDto(
        String groupLabel,
        Long groupId,
        List<CategoryDto> items
) {
}
