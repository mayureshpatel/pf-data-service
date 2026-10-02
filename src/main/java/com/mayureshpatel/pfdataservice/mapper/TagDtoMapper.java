package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagDto;

/** Converts the {@link Tag} domain object into its API-facing {@link TagDto}. */
public final class TagDtoMapper {

    private TagDtoMapper() {
    }

    /**
     * @param tag the domain tag to convert, or {@code null}
     * @return the equivalent DTO, or {@code null} if {@code tag} was {@code null}
     */
    public static TagDto toDto(Tag tag) {
        if (tag == null) return null;
        return new TagDto(
                tag.getId(),
                tag.getUserId(),
                tag.getName(),
                tag.getColor()
        );
    }
}
