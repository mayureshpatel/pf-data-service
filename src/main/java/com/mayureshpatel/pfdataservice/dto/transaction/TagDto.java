package com.mayureshpatel.pfdataservice.dto.transaction;

public record TagDto(
        Long id,
        Long userId,
        String name,
        String color
) {
}
