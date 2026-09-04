package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagDto;

public final class TagDtoMapper {

    private TagDtoMapper() {
    }

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
