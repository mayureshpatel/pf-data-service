package com.mayureshpatel.pfdataservice.dto.transaction.tags;

public record TagDto(
        Long id,
        Long userId,
        String name,
        String color
) {
}
