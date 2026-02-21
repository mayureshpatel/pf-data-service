package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.domain.user.User;

public record TagDto(
        Long id,
        User user,
        String name,
        Iconography iconography
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
                tag.getUser(),
                tag.getName(),
                tag.getIconography()
        );
    }
}
