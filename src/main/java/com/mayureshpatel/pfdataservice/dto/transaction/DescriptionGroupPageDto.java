package com.mayureshpatel.pfdataservice.dto.transaction;

import org.springframework.data.web.PagedModel;

import java.util.List;

/**
 * One page of description groups plus the total number of unlinked descriptions, so the Unlinked
 * tab can show its badge from the same response. The {@code content} and {@code page} fields have
 * the same shape as the app's other paged responses.
 *
 * @param content                  the groups on this page
 * @param page                     size, number, total groups and total pages
 * @param unlinkedDescriptionCount how many of the user's descriptions have no merchant link,
 *                                 regardless of the search
 */
public record DescriptionGroupPageDto(
        List<DescriptionGroupDto> content,
        PagedModel.PageMetadata page,
        long unlinkedDescriptionCount
) {
}
