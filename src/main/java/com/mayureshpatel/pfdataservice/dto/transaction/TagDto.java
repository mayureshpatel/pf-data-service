package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.Tag;

public record TagDto(
        Long id,
        Long userId,
        String name,
        String color
) {

    /**
     * Maps a {@link Tag} domain object to its corresponding DTO representation.
     *
     * @param tag The Tag domain object to be mapped.
     * @return The {@link TagDto} representation of the provided Tag.
     */
    public static TagDto mapToDto(Tag tag) {
        return new TagDto(
                tag.getId(),
                tag.getUser().getId(),
                tag.getName(),
                tag.getColor()
        );
    }
}
